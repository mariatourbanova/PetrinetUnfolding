package org.processmining.plugins.unfolding;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.converters.bpmn2pn.BPMN2WorkflowSystemConverter;
import org.processmining.plugins.converters.bpmn2pn.InfoConversionBP2PN;
import org.processmining.support.unfolding.StatisticMap;

/**
 * Converte un BPMNDiagram in una Occurrence net with Unfolding
 * 
 * @author Daniele Cicciarella
 */
public class BPMN2Unfolding_Plugin 
{
	@Plugin
	(
			name = "BCS BPMN to Unfolding net", 
			parameterLabels = {"BPMNDiagram"},
			returnLabels = {"Visualize BCS Unfolding Statistics", "Petri net", "Petri net"}, 
			returnTypes = {StatisticMap.class,Petrinet.class ,Petrinet.class}, 
			userAccessible = true, 
			help = "Convert BPMN diagram to Unfolding net"
			)
	@UITopiaVariant
	(
			affiliation = "University of Pisa", 
			author = "Daniele Cicciarella", 
			email = "cicciarellad@gmail.com"
			)
	public Object[] convert(PluginContext context, BPMNDiagram bpmn) 
	{

		Petrinet petrinet;
		BPMN2WorkflowSystemConverter bpmn2Petrinet;
		BCSUnfolding petrinet2Unfolding;
		Object[] unfolding;
		try{
			/* Settiamo la barra progressiva */
			setProgress(context, 0, 3);

			/* Converte il BPMN in rete di Petri */
			writeLog(context, "Conversion of the BPMN in Petri net...");
			bpmn2Petrinet = new BPMN2WorkflowSystemConverter(bpmn);
			bpmn2Petrinet.convert(context);
			petrinet = bpmn2Petrinet.getPetriNet();

			InfoConversionBP2PN info = bpmn2Petrinet.getInfoConversionBP2PN();

			/* Converte la rete di Petri nella rete di unfolding */
			writeLog(context, "Conversion of the Petri net in Unfolding net...");
			petrinet2Unfolding = new BCSUnfolding(context, petrinet);
			unfolding = petrinet2Unfolding.convert();

			/* Aggiungo connessione per la visualizzazione delle reti e statistiche delle rete unfoldata */

			context.addConnection(new BCSUnfoldingConnection((StatisticMap)unfolding[1], petrinet,(Petrinet) unfolding[0],info,bpmn));
			printstatistic(context,bpmn,petrinet, (Petrinet)unfolding[0]);

			return new Object [] {unfolding[1], unfolding[0], petrinet};
		}catch (Exception e) {
			context.log(e.getMessage());
			return null;
		}
	}

	private void printstatistic(PluginContext context, BPMNDiagram bpmn, Petrinet petrinet, Petrinet unfolding) {
		String message = String.format("Node/Flow: %s / %s", bpmn.getNodes().size(), bpmn.getFlows().size()+bpmn.getMessageFlows().size());
		context.log(message);
		message = String.format("T/P: %s / %s", petrinet.getTransitions().size(), petrinet.getPlaces().size());
		context.log(message);
		message = String.format("T/P: %s / %s", unfolding.getTransitions().size(), unfolding.getPlaces().size());
		context.log(message);

	}

	/**
	 * Setta gli step della barra progressiva
	 * 
	 * @param context contesto di ProM
	 * @param minimun minimo valore
	 * @param maximum massimo valore
	 */
	private void setProgress(PluginContext context, int minimun, int maximum)
	{
		context.getProgress().setMinimum(minimun);
		context.getProgress().setMaximum(maximum);
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
