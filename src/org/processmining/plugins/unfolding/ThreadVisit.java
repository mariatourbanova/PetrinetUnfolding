package org.processmining.plugins.unfolding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
	protected ConcurrentLinkedQueue <LocalConfiguration> queue = new ConcurrentLinkedQueue <LocalConfiguration>();
	protected HashMap <PetrinetNode, PetrinetNode> unf2PetriMap = new HashMap <PetrinetNode, PetrinetNode>();
	protected Petrinet petrinet, unfolding;
	protected HashMap <PetrinetNode, ArrayList<PetrinetNode>> petri2UnfMap = new HashMap <PetrinetNode, ArrayList<PetrinetNode>>();
	
	protected HashMap <PetrinetNode, ArrayList<PetrinetNode>> markingMap = new HashMap <PetrinetNode, ArrayList<PetrinetNode>>();
	
	protected StatisticMap statisticMap = null;
	protected LocalConfigurationMap localConfigurationMap = null;

	/* Variabili per la trasformazione della rete di Petri in N* */
	protected Place i, o;
	protected Transition reset;
	
	ThreadVisit(ConcurrentLinkedQueue <LocalConfiguration> queue, HashMap <PetrinetNode, PetrinetNode> unf2PetriMap, Petrinet petrinet, Petrinet unfolding,
			HashMap<PetrinetNode, ArrayList<PetrinetNode>> petri2UnfMap,
			HashMap <PetrinetNode, ArrayList<PetrinetNode>> markingMap,
			StatisticMap statisticMap, LocalConfigurationMap localConfigurationMap,
			Place i, Place o, Transition reset){
		this.queue = queue;
		this.unf2PetriMap = unf2PetriMap;
		this.petrinet = petrinet;
		this.petri2UnfMap = petri2UnfMap;
		this.unfolding = unfolding;
		this.markingMap = markingMap;
		this.statisticMap = statisticMap;
		this.localConfigurationMap = localConfigurationMap;
		this.i=i;
		this.o=o;
		this.reset=reset;
	}
	
	
	public void run(){
	while(!queue.isEmpty())
	{	
		/* Estraggo una configurazione c da q */
		LocalConfiguration c = queue.poll();
		/* Mappo da unfolding (t1) a petri (t) la prima transizione della configurazione */
		Transition t1 = c.get().get(0);
		Transition t = (Transition) unf2PetriMap.get(t1);

		/* Per ogni piazza p della rete originale attaccate a t */
		for(DirectedGraphEdge<?, ?> a1: petrinet.getGraph().getOutEdges(t))
		{

			Place p = (Place) a1.getTarget();
			Place pi = getPrecedent(t1, p);

			/* Per ogni transizione t2 delle rete originale attaccate a p */
			for(DirectedGraphEdge<?, ?> a2: petrinet.getGraph().getOutEdges(p))
			{
				Transition t2 = (Transition) a2.getTarget();
				PetrinetNode [] presetT2 = null;
				ArrayList <Combination> combination = null;
				int sizeCombination = 1;

				/* Verifico se t2 è abilitata */
				if((presetT2 = Utility.isEnabled(petrinet, t2, petri2UnfMap)) == null)
					continue;

				/* Prendo il preset di t2 per creare tutte le combinazioni possibili */
				ArrayList <ArrayList <PetrinetNode>> possibleCombination = new ArrayList <ArrayList <PetrinetNode>>();
				for(int i = 0; i < presetT2.length; i++)
				{
					if(!unf2PetriMap.get(pi).equals(presetT2[i])) 
					{
						ArrayList <PetrinetNode> array = petri2UnfMap.get(presetT2[i]);
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
				//System.out.println(possibleCombination);

				Combination.filter(combination, (Transition) t2, petri2UnfMap, unfolding);

				/* Per ogni combinazione rimanente */
				for(Combination comb : combination)
				{
					/* Aggiungo t2 all'unfolding il quale sarà collagato con le piazze che lo abilitano */
					Transition t3 = unfolding.addTransition(t2.getLabel());
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
						PetrinetNode [] postset = Utility.getPostset(petrinet, t2);

						// Verifico se una piazza finale di t2 è condivisa da altre transizioni e se provoca cutoff
						for(int i = 0; i < postset.length && !isCutoff; i++)
							isCutoff = isCutoff(t3, postset[i]);

						// Se t3 è un punto di cutoff la configurazione non deve essere aggiunta nella coda
						if(!isCutoff)
						{
							for(PetrinetNode p2: postset)
							{
								Place p3 = unfolding.addPlace(p2.getLabel());
								unfolding.addArc(t3, p3);						
								refreshCorrispondence(p2, p3);
							}
							queue.add(localConfigurationMap.get(t3));
						}
					}
				}
			}
		}
		//System.out.println(localConfigurationMap);
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
		petri2UnfMap.get(pn).add(pn1);
		unf2PetriMap.put(pn1, pn);

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
