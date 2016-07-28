package org.processmining.plugins.unfolding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.processmining.models.graphbased.directed.DirectedGraphEdge;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.support.localconfiguration.LocalConfiguration;
import org.processmining.support.localconfiguration.LocalConfigurationMap;
import org.processmining.support.unfolding.Combination;
import org.processmining.support.unfolding.StatisticMap;
import org.processmining.support.unfolding.Utility;

public class ThreadVisit implements Runnable {
	private ConcurrentLinkedQueue<LocalConfiguration> concurrentQueue;
	private Map<PetrinetNode, PetrinetNode> unf2PetriMap;
	private Petrinet petrinet, unfolding;
	private Map <PetrinetNode, ArrayList<PetrinetNode>> petri2UnfMap;
	private Transition reset;
	private Map <PetrinetNode, ArrayList<PetrinetNode>> markingMap;
	private StatisticMap statisticMap;
	private LocalConfigurationMap localConfigurationMap;

	public ThreadVisit(Transition reset, ConcurrentLinkedQueue<LocalConfiguration> concurrentQueue2, Map<PetrinetNode, PetrinetNode> unf2PetriMap, Petrinet petrinet, 
			Map<PetrinetNode,ArrayList<PetrinetNode>> petri2UnfMap, 
			Petrinet unfolding,
			StatisticMap statisticMap, Map<PetrinetNode, ArrayList<PetrinetNode>> markingMap, LocalConfigurationMap localConfigurationMap) {

		this.concurrentQueue = concurrentQueue2;
		this.unf2PetriMap = unf2PetriMap;
		this.petrinet = petrinet;
		this.petri2UnfMap = petri2UnfMap;
		this.unfolding = unfolding;
		this.reset = reset;
		this.markingMap = markingMap;
		this.statisticMap = statisticMap;
		this.localConfigurationMap = localConfigurationMap;
	}


	public void run(){
		{	
			while(!concurrentQueue.isEmpty())
			{	/* Estraggo una configurazione c da q */
				LocalConfiguration c = concurrentQueue.poll();
				if(c!=null){
					/* Mappo da unfolding (t1) a petri (t) la prima transizione della configurazione */
					Transition t1 = c.get().get(0);
					Transition t = (Transition) unf2PetriMap.get(t1);

					/* Per ogni piazza p della rete originale attaccate a t */
					for(DirectedGraphEdge<?, ?> a1: petrinet.getGraph().getOutEdges(t))
					{

						Place p = (Place) a1.getTarget();
						//p.getAttributeMap().put("Original id", a1.getAttributeMap().get("Original id"));
						Place pi = getPrecedent(t1, p);
						//pi.getAttributeMap().put("Original id", t1.getAttributeMap().get("Original id"));
						/* Per ogni transizione t2 delle rete originale attaccate a p */
						for(DirectedGraphEdge<?, ?> a2: petrinet.getGraph().getOutEdges(p))
						{
							Transition t2 = (Transition) a2.getTarget();
							List<PetrinetNode> presetT2 = null;
							ArrayList <Combination> combination = null;
							int sizeCombination = 1;

							/* Verifico se t2 è abilitata */
							if((presetT2 = Utility.isEnabled(petrinet, t2, petri2UnfMap)) == null)
								continue;

							/* Prendo il preset di t2 per creare tutte le combinazioni possibili */
							ArrayList <ArrayList <PetrinetNode>> possibleCombination = new ArrayList <ArrayList <PetrinetNode>>();
							for(int i = 0; i < presetT2.size(); i++)
							{
								if(!unf2PetriMap.get(pi).equals(presetT2.get(i))) 
								{
									ArrayList <PetrinetNode> array = petri2UnfMap.get(presetT2.get(i));
									possibleCombination.add(array);
									sizeCombination = sizeCombination * array.size();
								}
								else
								{
									ArrayList <PetrinetNode> array = new ArrayList <PetrinetNode> ();
									array.add(pi);
									possibleCombination.add(array);
								}
							}

							/* Crea le combinazioni e filtra quelle già usate */
							combination = new ArrayList <Combination> (sizeCombination);
							Combination.create(possibleCombination, combination);
						
							Combination.filter(combination, (Transition) t2, petri2UnfMap, unfolding);

							/* Per ogni combinazione rimanente */
							for(Combination comb : combination)
							{	
								String id = "";

								try{
									id = t2.getAttributeMap().get("Original id").toString();
								}catch(NullPointerException e){
									id = "_not_present";
								}
								Transition t3 = unfolding.addTransition(t2.getLabel());
								/* Aggiungo t2 all'unfolding il quale sarà collagato con le piazze che lo abilitano */

								t3.getAttributeMap().put("Original id",id);		
								for(int i = 0; i < comb.getElements().length; i++)
									unfolding.addArc((Place) comb.getElements()[i], t3);

								// Verifico se l'inserimento di t3 provaca conflitto in tal caso la elimino
								if(comb.isConflict(unfolding, t3))
								{
									unfolding.removeTransition(t3);
									continue;
								}

								refreshCorrispondence(t2, t3);

								/* Verifico se t3 provoca cutoff */
								if(t2.equals(reset))
								{ 
									if(markingMap.get(t3).size() == 0)
										statisticMap.addCutoff((Transition) t3);
									else  
										statisticMap.addCutoffUnbounded((Transition) t3);
								}
								else
								{							
									boolean isCutoff = false;
									List<PetrinetNode> postset = Utility.getPostset(petrinet, t2);

									// Verifico se una piazza finale di t2 è condivisa da altre transizioni e se provoca cutoff
									for(int i = 0; i < postset.size() && !isCutoff; i++)
										isCutoff = isCutoff(t3, postset.get(i));

									// Se t3 è un punto di cutoff la configurazione non deve essere aggiunta nella coda
									if(!isCutoff)
									{
										for(PetrinetNode p2: postset)
										{	
											Place p3 = unfolding.addPlace(p2.getLabel());
											p3.getAttributeMap().put("Original id",p2.getAttributeMap().get("Original id"));
											unfolding.addArc(t3, p3);									
											refreshCorrispondence(p2, p3);
										}
										concurrentQueue.add(localConfigurationMap.get(t3));
									}
								}
							}
						}
					}
				}
			}
		}


	}

	/**
	 * Prendo la piazza che precede la transizione nell'unfolding
	 * 
	 * @param t transizione delle rete di Petri
	 * @param p piazza della rete di Petri
	 * @return la piazza della rete di unfolding che precede t
	 */
	private Place getPrecedent(Transition t, Place p) 
	{
		Place pi = null;
		ArrayList<PetrinetNode> places = petri2UnfMap.get(p);

		for(int i = 0; i < places.size(); i++)
			if(unfolding.getArc(t, places.get(i)) != null)
				pi = (Place) places.get(i);
		return pi;
	}

	/**
	 * Aggiorna le corrispondenze delle map
	 * 
	 * @param pn nodo della rete di Petri
	 * @param pn1 nodo della rete di unfolding
	 */
	private void refreshCorrispondence(PetrinetNode pn, PetrinetNode pn1)
	{
		/* Aggiorno le map delle corrispondenze */


		if(!petri2UnfMap.containsKey(pn)) 
			petri2UnfMap.put(pn, new ArrayList<PetrinetNode>());		
		unf2PetriMap.put(pn1, pn);

		petri2UnfMap.get(pn).add(pn1);			


		/* Se è una transizione aggiornare le altre map */
		if(pn1 instanceof Transition)
		{

			localConfigurationMap.add(pn1, unfolding);				

			markingMap.put(pn1, Utility.getMarking(petrinet, localConfigurationMap.get(pn1), unf2PetriMap));

		}
	}

	/**
	 * Verifico se una transizione provoca il cutoff
	 * 
	 * @param t transizione da verificare
	 * @param place una piazza finale della transizione t
	 * @return true se la transizione è un cutoff, false altrimenti
	 */
	private boolean isCutoff(Transition t, PetrinetNode place) 
	{
		int isBounded;

		// Controllo se place è stato inserito nell'unfolding
		if(petri2UnfMap.containsKey(place))
		{
			ArrayList<PetrinetNode> markingT = markingMap.get(t);

			// Se nella storia dei place di t esiste place allora è un ciclo
			for(Place h : Utility.getHistoryPlace(unfolding, t))
			{
				if(unf2PetriMap.get(h).equals(place)) 
				{					
					for(DirectedGraphEdge<?, ?> a: unfolding.getGraph().getInEdges(h))
					{
						isBounded = Utility.isBounded(markingT, markingMap.get(a.getSource()));
						if(isBounded == 0)
						{
							statisticMap.addCutoff(t);
							return true;
						}
						else if(isBounded > 0) 
						{
							statisticMap.addCutoffUnbounded(t);
							return true;
						}
					}
				}
			}
			return false;
		}
		else
			return false;
	}
}
