package org.processmining.plugins.unfolding;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.plugins.converters.bpmn2pn.InfoConversionBP2PN;
import org.processmining.plugins.unfolding.visualize.StringPanel;
import org.processmining.plugins.unfolding.visualize.TabTraceUnfodingPanel;
import org.processmining.support.localconfiguration.LocalConfigurationMap;
import org.processmining.support.unfolding.LegendBCSUnfolding;
import org.processmining.support.unfolding.StatisticMap;
import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;


public class MyBCSUnfoldingVisualizePlugin {

	private StatisticMap output;
	private StatisticMap statBPMN;
	private UIPluginContext context; 
	private InfoConversionBP2PN info = null;
	private static BPMNDiagram bpmn= null;
	private BPMNDiagram bpmncopia; 


	private LocalConfigurationMap local;
//	private static BPMNDiagram bpmncopia = null;
	private Petrinet petrinet;
	private Petrinet unfolding = null;
	private JPanel panel;
	private Map<PetrinetNodeMod,BPMNNode> reverseMap;
	
	@Plugin
	(
			name = "Updated Visualization BCS Unfolding Statistics", 
			returnLabels = { "Visualize BCS Unfolding Statistics" }, 
			parameterLabels = { "Visualize BCS Unfolding Statistics" }, 
			returnTypes = { JComponent.class }, 
			userAccessible = true,
			help = "Visualize BCS Unfolding Statistics"
			)
	@UITopiaVariant
	(
			affiliation = "University of Pisa", 
			author = "Francesco Boscia", 
			email = "francesco.boscia@gmail.com"
			)
	@Visualizer
	public JComponent runUI(UIPluginContext context, StatisticMap output) 
	{
		panel = new JPanel();

		this.output = output;
		this.context = context;
		this.statBPMN = output;
		reverseMap = output.getReverseMap();
		try 
		{	
			/* Carico le reti utilizzando la connessione creata in precedenza */
			BCSUnfoldingConnection unfoldingConnection = context.getConnectionManager().getFirstConnection(BCSUnfoldingConnection.class, context, output);
			petrinet = unfoldingConnection.getObjectWithRole(BCSUnfoldingConnection.PETRINET);
			unfolding = unfoldingConnection.getObjectWithRole(BCSUnfoldingConnection.UNFOLDING);
			try{
				bpmn = unfoldingConnection.getObjectWithRole(BCSUnfoldingConnection.BPMN);
				local =unfoldingConnection.getObjectWithRole(BCSUnfoldingConnection.LocalConfiguration);
				info = unfoldingConnection.getObjectWithRole(BCSUnfoldingConnection.InfoCBP2PN);
			}catch (Exception e) {
				bpmn = null;
				info = null;
			}
			
			BPMNDiagram bpmncopia= insertDefect(bpmn,output);
		repaint( true,bpmncopia);
		 } 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return panel;
	}
	
	public void repaint( boolean flag, BPMNDiagram bpmncopia) {
		try{
			double size [] [] = {{TableLayoutConstants.FILL} , {TableLayoutConstants.FILL,TableLayoutConstants.FILL}};
			panel.setLayout(new TableLayout(size));
			

			/*Costruisco le statistiche del BPMN graph*/
			statBPMN.setStatistic(bpmn);
			
			/*Costruisco il pannello del BPMN e il ViewInteraction Panel della legenda*/
			ProMJGraphPanel bpmnPanel = ProMJGraphVisualizer.instance().visualizeGraph(context,bpmncopia);
			LegendBCSUnfolding legendPanelB = new LegendBCSUnfolding(bpmnPanel, "Legend");
			bpmnPanel.addViewInteractionPanel(legendPanelB, SwingConstants.EAST);
			panel.add(bpmnPanel, "0,0");

			StringPanel sp1 = new StringPanel(bpmnPanel, "Statistic BPMN", statBPMN.getBPMNStatistic());
			bpmnPanel.addViewInteractionPanel(sp1, SwingConstants.SOUTH);
			panel.revalidate();
			panel.repaint();
			
			/*Sostituire la history con la localConfiguration*/
			HistoryUnfolding hu = new HistoryUnfolding(unfolding);

			/*costruzione del widget inspector*/
			
			if(flag){
				TabTraceUnfodingPanel tabunf = new TabTraceUnfodingPanel(context, bpmnPanel, "History Unfolding", hu, output, this, bpmn, info, local);
			}
			
			/*Costruisco il pannello dell'Unfolding*/
			ProMJGraphPanel unfoldingPanel = ProMJGraphVisualizer.instance().visualizeGraph(context, unfolding);
			LegendBCSUnfolding legendPanelP = new LegendBCSUnfolding(unfoldingPanel, "Legend");
			unfoldingPanel.addViewInteractionPanel(legendPanelP, SwingConstants.EAST);
			panel.add(unfoldingPanel, "0,1");
			
			/*Costruisco il ViewInteraction pannello delle statistiche*/
			StringPanel sp = new StringPanel(unfoldingPanel, "Statistic Unfolding", output.getStatistic());
			unfoldingPanel.addViewInteractionPanel(sp, SwingConstants.SOUTH);
			panel.revalidate();
			panel.repaint();
		}catch (Exception e) 
		{
			e.printStackTrace();
		}

	}
	
	public BPMNDiagram getBpmncopia() {
		
		
		return insertDefect(bpmn,output);
	}


	public static BPMNDiagram getOriginalBpmn(){
		return bpmn;
	}
	
	public Petrinet getPetriNet(){
		return petrinet;
	}
	
	private static boolean confrontoBPMNnode(BPMNNode bpmnNode, BPMNNode bn){
		if (bpmnNode != null && bn != null){
			if ((bpmnNode.getLabel()).equals(bn.getLabel())) return true;
		}return false;
	}
	
	public static BPMNNode getNodeinClone(BPMNDiagram bpmn,BPMNNode node){
		Set<BPMNNode> elenco = bpmn.getNodes();
		for(BPMNNode nodeclone: elenco){
			if(nodeclone.getLabel()!=null)
				if(confrontoBPMNnode(nodeclone,node)){
					return nodeclone;
			}
		}
		return null;
	}

	
private BPMNDiagram insertDefect(BPMNDiagram bpmnoriginal, StatisticMap map) {
		//Clono il BPMN diagram
		bpmncopia = BPMNDiagramFactory.cloneBPMNDiagram(bpmnoriginal);
		 
		for( Transition t: map.getCutoff()){
			BPMNNode bpnode = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,t);
			if (bpnode != null){		
				getNodeinClone(bpmncopia,bpnode).getAttributeMap().put(AttributeMap.FILLCOLOR, Color.BLUE);}
			else System.out.println("vuoto");
			
		}

		for( Transition t: map.getCutoffUnbounded()){
			BPMNNode bpnode = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,t);
			if (bpnode != null){
			getNodeinClone(bpmncopia,bpnode).getAttributeMap().put(AttributeMap.FILLCOLOR, Color.BLUE);}
			else System.out.println("vuoto");
		}

		for( Transition t: map.getDeadlock()){
			BPMNNode bpnode = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,t);
			if (bpnode != null){
				BPMNNode bpnod = getNodeinClone(bpmncopia,bpnode);
				Color colo = (Color) bpnod.getAttributeMap().get(AttributeMap.FILLCOLOR);
				if(colo.equals(Color.BLUE)){
					Color violet = new Color(138,43,226);
					bpnod.getAttributeMap().put(AttributeMap.FILLCOLOR, violet);
					
				}else
					bpnod.getAttributeMap().put(AttributeMap.FILLCOLOR, Color.RED);
				
				System.out.println("vuoto");
			}
				
			
			else System.out.println("vuoto");			
		}

		return bpmncopia;
	
}
}
