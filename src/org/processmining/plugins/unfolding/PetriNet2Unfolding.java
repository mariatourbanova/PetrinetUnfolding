package org.processmining.plugins.unfolding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.DirectedGraphEdge;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.support.localconfiguration.LocalConfiguration;
import org.processmining.support.localconfiguration.LocalConfigurationMap;
import org.processmining.support.unfolding.StatisticMap;
import org.processmining.support.unfolding.Pair;
import org.processmining.support.unfolding.PetrinetNodeTupla;
import org.processmining.support.unfolding.Utility;

/**
 * Converte un rete di Petri in una rete di unfolding
 * 
 * @author Daniele Cicciarella
 */
public class PetriNet2Unfolding 
{	
	/* Contesto di ProM */
	protected PluginContext context;
	
	/* Reti di petri */
	protected Petrinet petrinet, unfolding;
	
	/* Variabili per la trasformazione della rete di Petri in N* */
	protected Place i, o;
	protected Transition reset;
		
	/* Coda contenente le configurazioni da analizzare */
	protected LinkedList <LocalConfiguration> queue = new LinkedList <LocalConfiguration>();
	
	/* Mappa ogni nodo della rete di Petri a un uno o più nodi della rete di unfolding */
	protected HashMap <PetrinetNode, ArrayList<PetrinetNode>> petri2UnfMap = new HashMap <PetrinetNode, ArrayList<PetrinetNode>>();
	
	/* Mappa ogni nodo della rete di unfolding a un nodo della rete di Petri */
	protected HashMap <PetrinetNode, PetrinetNode> unf2PetriMap = new HashMap <PetrinetNode, PetrinetNode>();
	
	/* Mappa ogni transizione della rete di unfolding con il rispettivo marking */
	protected HashMap <PetrinetNode, ArrayList<PetrinetNode>> markingMap = new HashMap <PetrinetNode, ArrayList<PetrinetNode>>();
	
	/* Mappa ogni transizione la storia dei suoi xor-split  */
	protected HashMap <PetrinetNode, ArrayList<Pair>> xorMap = new HashMap <PetrinetNode, ArrayList<Pair>>();
	
	/* Mappa le configurazioni locali di ogni transizione delle rete di unfolding */
	protected LocalConfigurationMap localConfigurationMap = new LocalConfigurationMap();
	
	/* Mappa i livelock e deadlock e altre statistiche */
	protected StatisticMap statisticMap = new StatisticMap();
	
	/**
	 * Costruttore
	 * 
	 * @param context contesto di ProM
	 * @param petrinet rete di petri originale
	 */
	PetriNet2Unfolding(PluginContext context, Petrinet petrinet) 
	{
		this.context = context;
		this.petrinet = petrinet;
		this.unfolding = PetrinetFactory.newPetrinet("Unfolding from Petrinet");		
	}
	
	/**
	 * Converte una rete di Petri in una rete di unfolding
	 * 
	 * @return la rete di unfolding e le sue statistiche
	 */
	public Object[] convert() 
	{
		i = (Place) Utility.getStartNode(petrinet); 
		o = (Place) Utility.getEndNode(petrinet);

		/* Inizio la costruzione della rete inserendo la piazza iniziale i1 */
		Place i1 = unfolding.addPlace("start");	
		addCorrispondence(i, i1);
		
		/* Trasformo la rete di Petri N in N* */
		reset = petrinet.addTransition("reset");
		petrinet.addArc(o, reset);
		petrinet.addArc(reset, i);
		
		/* Inizializzo e visito la coda */
		initQueue(i, i1);		
		visitQueue();	
		
		/* Estraggo i deadlock ed effettuo le statistiche della rete */
		writeLog(context, "Extraction of the dealock points...");
		getStatistics();
		
		return new Object [] {unfolding, statisticMap};
	}

	/**
	 * Inizializzazione della coda di priorità
	 * 
	 * @param p piazza iniziale della rete di petri
	 * @param p1 piazza iniziale della rete di unfolding
	 */
	private void initQueue(Place p, Place p1) 
	{
		/* Per tutte le transizioni t della rete di petri attaccate alla piazza iniziale p */
		for(DirectedGraphEdge<?, ?> a1: petrinet.getGraph().getOutEdges(p))
		{
			/* Creo una transizione t1 nell'unfolding e attacco p1 con t1 */
			Transition t = (Transition) a1.getTarget();
			Transition t1 = unfolding.addTransition(t.getLabel());
			unfolding.addArc(p1, t1);			
			
			/* Per tutti i place u delle rete di petri attaccate a t */
			for(DirectedGraphEdge<?, ?> a2: petrinet.getGraph().getOutEdges(t))
			{
				// Creo un place u1 nell'unfolding e attacco t1 con u1
				Place u = (Place) a2.getTarget();
				Place u1 = unfolding.addPlace(u.getLabel());
				unfolding.addArc(t1, u1);				
				addCorrispondence((PetrinetNode) u, u1);
			}

			/* Aggiorno tutte le strutture globali e la coda */
			addCorrispondence(t, t1);
			queue.push(localConfigurationMap.get(t1));
		}
	}

	/**
	 * Visito la coda di priorità per la creazione della rete di Petri
	 */
	private void visitQueue() 
	{		
		while(!queue.isEmpty())
		{	
			/* Estraggo una configurazione c da q */
			LocalConfiguration c = queue.pop();
			
			/* Mappo da unfolding (t1) a petri (t) la prima transizione della configurazione */
			Transition t1 = c.get().get(0);
			Transition t = (Transition) unf2PetriMap.get(t1);
					
			/* Per ogni piazza p della rete originale attaccate a t */
			for(DirectedGraphEdge<?, ?> a1: petrinet.getGraph().getOutEdges(t))
			{
			
				Place p = (Place) a1.getTarget(), pi = null;
				for(int i = 0; i < petri2UnfMap.get(p).size(); i++)
					if(unfolding.getArc(t1, petri2UnfMap.get(p).get(i)) != null)
						pi = (Place) petri2UnfMap.get(p).get(i);
				
				/* Per ogni transizione t2 delle rete originale attaccate a p */
				for(DirectedGraphEdge<?, ?> a2: petrinet.getGraph().getOutEdges(p))
				{
					Transition t2 = (Transition) a2.getTarget();
					ArrayList<PetrinetNodeTupla> combination = null;
					ArrayList <PetrinetNode> presetT2 = null;
					
					/* Verifico se t2 è abilitata */
					if((presetT2 = Utility.isEnabled(petrinet, t2, petri2UnfMap)) == null)
						continue;
					
					/* Calcolo tutte le combinazioni possibili in ingresso a t2 filtrando quelle usate */
					ArrayList <ArrayList <PetrinetNode>> comb = new ArrayList <ArrayList <PetrinetNode>>();
					for(int i = 0; i < presetT2.size(); i++)
					{
						if(!unf2PetriMap.get(pi).equals(presetT2.get(i))) 
						{
							ArrayList <PetrinetNode> array = petri2UnfMap.get(presetT2.get(i));
							comb.add(array);
						}
						else
						{
							ArrayList <PetrinetNode> array = new ArrayList <PetrinetNode> ();
							array.add(pi);
							comb.add(array);
						}
					}
					combination = PetrinetNodeTupla.createCombination(comb);					
					PetrinetNodeTupla.filterCombination(combination, (Transition) t2, petri2UnfMap, unfolding);

					/* Per ogni combinazione rimanente */
					for(int i = 0; i < combination.size(); i++)
					{					
						/* Aggiungo t2 all'unfolding il quale sarà collagato con le piazze che lo abilitano */
						Transition t3 = unfolding.addTransition(t2.getLabel());
						for(int j = 0; j < combination.get(i).getElements().length; j++)
							unfolding.addArc((Place) combination.get(i).getElements()[j], t3);
						
						// Verifico se l'inserimento di t3 provaca conflitto in tal caso la elimino
						if(isConflict(combination.get(i).getElements(), t3))
						{
							for(int j = 0; j < combination.get(i).getElements().length; j++)
								unfolding.removeArc((Place) combination.get(i).getElements()[j], t3);
							unfolding.removeTransition(t3);
							continue;
						}	
						addCorrispondence(t2, t3);
						
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
							boolean isCutOff = false;
							ArrayList <PetrinetNode> postset = Utility.getPostset(petrinet, t2);
							
							// Verifico se una piazza finale di t2 è condivisa da altre transizioni e se provoca cutoff
							for(PetrinetNode p2: postset)
								if(isCutOff = isCutOff(t3, p2))
									break;
							
							// Se t3 è un punto di cutoff la configurazione non deve essere aggiunta nella coda
							if(!isCutOff)
							{
								for(PetrinetNode p2: postset)
								{
									Place p3 = unfolding.addPlace(p2.getLabel());
									unfolding.addArc(t3, p3);						
									addCorrispondence(p2, p3);
								}
								queue.push(localConfigurationMap.get(t3));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Verifica se i place sono in conflitto
	 * 
	 * @param t transizione da verificare
	 * @param nodes array contenente i place da contrallare
	 * @return true se sono in conflitto, false altrimenti
	 */
	private boolean isConflict(PetrinetNode[] nodes, Transition t) 
	{
		ArrayList <Pair> XOR = new ArrayList <Pair> (), nodeXOR = null;
		
		for(int i = 0; i < nodes.length; i++)
		{
			nodeXOR = Utility.getHistoryXOR(unfolding, nodes[i], unfolding.getArc(nodes[i], t));
			if(!nodeXOR.isEmpty())
			{
				/* Se due piazze condividono lo stesso xor ma provengono da percorsi diversi è un conflitto */
				if(Utility.isConflict(XOR, nodeXOR))
					return true;

				for(int j = 0; j < nodeXOR.size(); j++)
					if(!XOR.contains(nodeXOR.get(j)))
						XOR.add(nodeXOR.get(j));
			}
		}
		return false;
	}
	
	/**
	 * Verifico se una transizione provoca il cutoff
	 * 
	 * @param t transizione da verificare
	 * @param placeFinal una piazza finale della transizione t
	 * @return true se la transizione è un cutoff, false altrimenti
	 */
	private boolean isCutOff(Transition t, PetrinetNode placeFinal) 
	{
		PetrinetNode transitionFinal = null;
		int isBounded;
		
		// Controllo se placeFinal è stato inserito
		if(!petri2UnfMap.containsKey(placeFinal))
			return false;
		else
		{
			for(Place h : Utility.getHistoryPlace(unfolding, t))
			{
				// Se nella storia dei place di t esiste placeFinal allora è un ciclo
				if(unf2PetriMap.get(h).equals(placeFinal)) 
				{					
					for(DirectedGraphEdge<?, ?> a: unfolding.getGraph().getInEdges(h))
					{
						transitionFinal = (PetrinetNode) a.getSource();	
						isBounded = Utility.isBounded(markingMap.get(t), markingMap.get(transitionFinal));
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
		}
		return false;
	}

	
	/**
	 * Estraggo i deadlock ed effettuo le statistiche della rete
	 */
	private void getStatistics()
	{		
		/* Inserisco i livelock trovati in un lista */
		ArrayList <Transition> cutoff = new ArrayList <Transition> ();
		for(int i = 0; i < statisticMap.getCutoff().size(); i++)
			cutoff.add(statisticMap.getCutoff().get(i));
		for(int i = 0; i < statisticMap.getCutoffUnbounded().size(); i++)
			cutoff.add(statisticMap.getCutoffUnbounded().get(i));
		
		/* Filtro i punti di cutoff per ottenere un primo insieme di spoilers */
		ArrayList<Transition> spoilers = filterCutoff(cutoff);
		
		/* Individuo i deadlock */
		ArrayList <Transition> deadlock = getDeadlock(cutoff, spoilers);		
		if(deadlock != null)
			statisticMap.setDeadlock(deadlock);
		
		/* Inserisco le altre statistiche */
		statisticMap.setStatistic(unfolding);
	}

	/**
	 * @param cutoff
	 * @return
	 */
	private ArrayList<Transition> filterCutoff(ArrayList<Transition> cutoff) 
	{
		ArrayList <Transition> cutoffHistory = new ArrayList <Transition> (), filter = new ArrayList <Transition>();

		/* */
		for(Transition v: cutoff)
		{ 
			for(Transition u: localConfigurationMap.get(v).get())
				if(!cutoffHistory.contains(u))
					cutoffHistory.add(u);
		}
		
		/* */
		for(Place p : unfolding.getPlaces())
		{
			if(unfolding.getGraph().getOutEdges(p).size() > 1)
			{
				for (Iterator<?> i = unfolding.getGraph().getOutEdges(p).iterator(); i.hasNext();) 
				{
					Arc a = (Arc) i.next();
					Transition t = (Transition) a.getTarget();
					if(!filter.contains(t) && !cutoffHistory.contains(t))
						filter.add(t);
				}
			}
		}
		return filter;		
	}

	/**
	 * Estrae i punti di deadlock
	 * 
	 * @param cutoff: arraylist contenente i punti di cutoff
	 * @return arraylist contenente i punti di deadlock
	 */
	private ArrayList<Transition> getDeadlock(ArrayList<Transition> cutoff, ArrayList<Transition> spoilers) 
	{
		Transition s = null;
		ArrayList <Transition> deadlock = null, cutoff1 = null, spoilers2 = null;

		if(!cutoff.isEmpty())
		{		
			Transition t = cutoff.get(0);
			ArrayList<Transition> spoilers1 = getSpoilers(t, spoilers);	
			while(!spoilers1.isEmpty() && deadlock == null)
			{
				s = spoilers1.remove(0);
				cutoff1 = removeConflict(cutoff, s);
				spoilers2 = removeConflict(spoilers, s);
				if(cutoff1.isEmpty())
				{
					deadlock = new ArrayList <Transition>();
					deadlock.add(s);
				}
				else
				{
					deadlock = getDeadlock(cutoff1, spoilers2);
					if(deadlock != null)
						deadlock.add(s);
				}
			}
			return deadlock;
		}
		else
			return null;
	}

	/**
	 * Prendo tutte le transizioni che sono in conflitto con il cutoff
	 * 
	 * @param t: cutoff scelto
	 * @return spoilers: arraylist di transizioni contenenti tutte le transizioni in conflitto con il cutoff
	 */
	private ArrayList<Transition> getSpoilers(Transition t, ArrayList<Transition> set) 
	{
		ArrayList<Transition> spoilers = new ArrayList<Transition>();
		
		/* Se sono in conflitto le aggiungo alla nuova lista */
		for(Transition t1: set)
			if(Utility.isConflict(xorMap.get(t), xorMap.get(t1)))
				spoilers.add(t1);
		
		return spoilers;
	}
	
	/**
	 * Scelto come nuovo insieme quelle che non sono in conflitto con lo spoiler
	 * 
	 * @param cutoff: insieme corrente di cutoff
	 * @param spoiler: spoiler corrente
	 * @return cutoff1: arraylist di transizioni contenente la nuova lista di cutoff
	 */
	private ArrayList<Transition> removeConflict(ArrayList<Transition> cutoff, Transition spoiler) 
	{	
		ArrayList<Transition> cutoff1 = new ArrayList<Transition>();
		
		/* Se le transizioni del cutoff non sono in conflitto con lo spoiler le aggiungo alla nuova lista */
		for(Transition t: cutoff)
			if(t != spoiler && !Utility.isConflict(xorMap.get(t), xorMap.get(spoiler)))
				cutoff1.add(t);
		
		return cutoff1;
	}
	
	/**
	 * Aggiunge le corrispondenze delle map
	 * 
	 * @param pn nodo della rete di Petri
	 * @param pn1 nodo della rete di Unfolding
	 */
	private void addCorrispondence(PetrinetNode pn, PetrinetNode pn1)
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
			xorMap.put(pn1, Utility.getHistoryXOR(unfolding, pn1, null));
		}
	}
	
	/**
	 * Scrive un messaggio di log e incrementa la barra progressiva
	 * 
	 * @param context contesto di ProM
	 * @param log messaggio di log
	 */
	private void writeLog(PluginContext context, String log)
	{
		context.log(log);
		context.getProgress().inc();
	}
}