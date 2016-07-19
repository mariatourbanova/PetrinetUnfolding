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
import org.processmining.models.graphbased.EdgeID;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.plugins.bpmn.BpmnEventBasedGateway;
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
	private BPMNDiagram bpmn= null;
	private CloneBPMN bpmncopia; 
	private LocalConfigurationMap local;
	private Petrinet petrinet;
	private Petrinet unfolding = null;
	private JPanel panel;
	private Map<PetrinetNodeMod,BPMNNode> reverseMap;
	private Palette pal = new Palette();
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
				
			}catch (Exception e) {
				bpmn = null;
				
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
			
			/*costruzione del widget inspector*/
			if(flag){
				TabTraceUnfodingPanel tabunf = new TabTraceUnfodingPanel(context, bpmnPanel, "History Unfolding",  output, this, bpmn, local);
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


	public BPMNDiagram getOriginalBpmn(){
		return bpmn;
	}
	
	public Petrinet getPetriNet(){
		return petrinet;
	}
	
	public BPMNNode getNodeinClone(BPMNDiagram bpmn,BPMNNode node){
		Set<BPMNNode> elenco = bpmn.getNodes();
		if(node!=null)
		for(BPMNNode nodeclone: elenco){
			Object idoc = nodeclone.getAttributeMap().get("Original id");
			Object inode = node.getAttributeMap().get("Original id");
			if( idoc.toString().equals(inode.toString())){
				return nodeclone;
			}
		}
		return null;
	}
	
	public BPMNEdge<BPMNNode, BPMNNode> getArcInClone(BPMNDiagram bpmn,BPMNEdge<BPMNNode, BPMNNode> arc){
		Set<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> elencoArchi = bpmn.getEdges();
		if(arc!=null)
		for(BPMNEdge<? extends BPMNNode, ? extends BPMNNode> arcClone: elencoArchi){
			Object idoc = arcClone.getAttributeMap().get("Original id");
			Object inode = arc.getAttributeMap().get("Original id");
			if( idoc.toString().equals(inode.toString())){
				return (BPMNEdge<BPMNNode, BPMNNode>) arcClone;
			}
		}
		return null;
	}
	/*
	public BPMNEdge<? extends BPMNNode, ? extends BPMNNode> getEdgeinClone(BPMNDiagram bpmn,BPMNEdge<BPMNNode, BPMNNode> f){
		 Set<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> elenco = bpmn.getEdges();
		if(f!=null)
		for(BPMNEdge<? extends BPMNNode, ? extends BPMNNode> flowclone: elenco){
			//prendere original Id dall'edge
			Object idoc = flowclone.getAttributeMap().get("Original id");
			
			//Object inode = f.getAttributeMap().get("Original id");
			System.out.println("idoc = " + idoc.toString() + " f = " + f.toString()  );
			//confrontare Origianl Id
			if( idoc.toString().equals(f.toString())){
				//ritornare edge clonato
				return flowclone;
			}
		}
		return null;
	}
*/
	
private BPMNDiagram insertDefect(BPMNDiagram bpmnoriginal, StatisticMap map) {
		//Clono il BPMN diagram
	
		bpmncopia = new CloneBPMN(bpmnoriginal.getLabel());
		bpmncopia.cloneFrom(bpmnoriginal);
		
		 
		for( Transition t: map.getCutoff()){
			BPMNNode bpnode = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,t);
			if (bpnode != null){		
				getNodeinClone(bpmncopia,bpnode).getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getCutColor());}
			else System.out.println("vuoto");
			
		}

		for( Transition t: map.getCutoffUnbounded()){
			BPMNNode bpnode = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,t);
			if (bpnode != null){
			getNodeinClone(bpmncopia,bpnode).getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getCutColor());}
			else System.out.println("vuoto");
		}

		for( Transition t: map.getDeadlock()){
			BPMNNode bpnode = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,t);
			if (bpnode != null){
				BPMNNode bpnod = getNodeinClone(bpmncopia,bpnode);
				Color colo = (Color) bpnod.getAttributeMap().get(AttributeMap.FILLCOLOR);
				if(colo != null) {
					if (colo.equals(pal.getCutColor())){
						bpnod.getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getBothCutoffDead());
					}
					else{					
						bpnod.getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getDeadColor());
					}}
					else{
						bpnod.getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getDeadColor());
					}
				
				System.out.println("vuoto");
			}
			else System.out.println("vuoto");			
		}

		return bpmncopia;
	
}
}
