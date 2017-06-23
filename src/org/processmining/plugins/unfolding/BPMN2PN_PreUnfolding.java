package org.processmining.plugins.unfolding;

import java.util.ArrayList;
import java.util.List;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.converters.bpmn2pn.BPMN2WorkflowSystemConverter;

/**
 * Converte un BPMNDiagram in una Occurrence net with Unfolding
 * 
 * @author Daniele Cicciarella, Giorgio O. Spagnolo
 */

@Plugin
(
		name = "BCS BPMN to Petri net", 
		parameterLabels = {"BPMNDiagram"},
		returnLabels = {"Petri net", "Conversion Error"}, 
		returnTypes = {Petrinet.class, List.class}, 
		userAccessible = true, 
		help = "Convert BPMN diagram to petri net"
		)
public class BPMN2PN_PreUnfolding 
{
	
	@UITopiaVariant
	(
			affiliation = "University of Pisa, ISTI CNR Italy", 
			author = "Daniele Cicciarella, Giorgio O. Spagnolo", 
			email = "cicciarellad@gmail.com, spagnolo@isti.cnr.it"
			)
	@PluginVariant(variantLabel = "BCS BPMN to pn", requiredParameterLabels = { 0 })
	public Object[] convert(PluginContext context, BPMNDiagram bpmn) throws Exception 
	{
		
		return convertmethod(context, bpmn);
	}
	
	
	
	private Object[] convertmethod(PluginContext context, BPMNDiagram bpmn)  
	{
		
		BPMN2WorkflowSystemConverter bpmn2Petrinet  = new BPMN2WorkflowSystemConverter(bpmn);;
		try {
		
			/* Settiamo la barra progressiva */
			setProgress(context, 0, 3);

			/* Converte il BPMN in rete di Petri */
			writeLog(context, "Conversion of the BPMN in Petri net...");
			bpmn2Petrinet = new BPMN2WorkflowSystemConverter(bpmn);
			bpmn2Petrinet.convert(context);
		
			
			

		
			
			return new Object [] {bpmn2Petrinet.getPetriNet(), bpmn2Petrinet.getErrors()};
			
			
		}catch(Exception e){
			return new Object [] {null, bpmn2Petrinet.getErrors()};
		}
		
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