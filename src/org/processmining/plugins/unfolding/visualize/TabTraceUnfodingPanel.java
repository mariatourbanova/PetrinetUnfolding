package org.processmining.plugins.unfolding.visualize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.DirectedGraphEdge;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.unfolding.CloneBPMN;
import org.processmining.plugins.unfolding.MyBCSUnfoldingVisualizePlugin;
import org.processmining.plugins.unfolding.Palette;
import org.processmining.plugins.unfolding.PetrinetNodeMod;
import org.processmining.plugins.unfolding.UtilitiesforMapping;
import org.processmining.support.localconfiguration.LocalConfiguration;
import org.processmining.support.localconfiguration.LocalConfigurationMap;
import org.processmining.support.unfolding.StatisticMap;
import com.fluxicon.slickerbox.components.AutoFocusButton;
import com.fluxicon.slickerbox.factory.SlickerDecorator;
import com.fluxicon.slickerbox.factory.SlickerFactory;

public class TabTraceUnfodingPanel extends JPanel implements MouseListener, MouseMotionListener, ViewInteractionPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */


	protected SlickerFactory factory = SlickerFactory.instance();
	protected SlickerDecorator decorator = SlickerDecorator.instance();
	private JComponent component;
	private JTable tab;
	private String panelName;
	private LocalConfigurationMap local;
	private UnfoldingInspectorPanel inspector;
	private StatisticMap statistiunf;
	private MyBCSUnfoldingVisualizePlugin visualizeUnfoldingStatistics_Plugin;
	private Map<LocalConfiguration,Color> mapLocalColor;
	private Map<PetrinetNodeMod,BPMNNode> reverseMap;
	private ArrayList<LocalConfiguration> list;
	private String elencoBPMN = "";
	private Palette pal = new Palette();
	private ArrayList<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> archi = new ArrayList<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>>();


	public TabTraceUnfodingPanel(PluginContext context, ScalableViewPanel panel, String panelName,
			StatisticMap statistiunf, MyBCSUnfoldingVisualizePlugin visualizeUnfoldingStatistics_Plugin, BPMNDiagram bpmn, LocalConfigurationMap local ){
		super(new BorderLayout());
		mapLocalColor = new HashMap<LocalConfiguration,Color>();
		list = new ArrayList<LocalConfiguration>();
		this.statistiunf = statistiunf;
		this.visualizeUnfoldingStatistics_Plugin = visualizeUnfoldingStatistics_Plugin;
		this.setBorder(BorderFactory.createEmptyBorder());
		this.setOpaque(true);
		this.setSize(new Dimension(160, 260));
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.local = local; 
		this.reverseMap = statistiunf.getReverseMap();
		panel.getViewport();
		init();
		this.panelName = panelName;
		inspector = new UnfoldingInspectorPanel(context);
		panel.add(inspector);
		painttabtrace();
		widget(this);
	}

	public JPanel widget(JPanel panell){

		JPanel comprisePanel = new JPanel();

		comprisePanel.setAlignmentX(LEFT_ALIGNMENT);
		comprisePanel.setBorder(BorderFactory.createEmptyBorder());
		comprisePanel.setOpaque(false);
		comprisePanel.setLayout(new BoxLayout(comprisePanel, BoxLayout.X_AXIS));
		comprisePanel.add(tab);
		comprisePanel.add(Box.createHorizontalGlue());

		inspector.addInfo("Unfolding History", comprisePanel);   

		JButton	button2  = new AutoFocusButton("Reset");
		button2.setOpaque(false);
		button2.setFont(new Font("Monospaced", Font.PLAIN, 12));
		button2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BPMNDiagram bpmncopia= visualizeUnfoldingStatistics_Plugin.getBpmncopia();
				visualizeUnfoldingStatistics_Plugin.repaint(false, bpmncopia);
			}
		}
				);

		inspector.addReset("Reset", button2);
		return comprisePanel;
	}


	private void init(){
		ArrayList<Transition> cutoff = statistiunf.getCutoff();
		ArrayList<Transition> cutoffU = statistiunf.getCutoffUnbounded();		
		ArrayList<Transition> dead = statistiunf.getDead();
		ArrayList<Transition> deadL = statistiunf.getDeadlock();

		fillmap(cutoff,pal.getCutColor());
		fillmap(cutoffU,pal.getCutColor());
		fillmap(dead,pal.getDeadColor());
		fillmap(deadL,pal.getDeadColor());		
	}

	public BPMNDiagram paintConf(LocalConfiguration localConfiguration,Map<?,BPMNNode> reverseMap){
		BPMNDiagram bpmnoriginal = visualizeUnfoldingStatistics_Plugin.getOriginalBpmn();
		CloneBPMN bpmncopia = new CloneBPMN(bpmnoriginal.getLabel());
		bpmncopia.cloneFrom(bpmnoriginal);
		BPMNEdge<BPMNNode, BPMNNode> arcoDead = null;
		BPMNNode node = null;
		BPMNNode previousNode = null;

		List<PetrinetNode> path = extractPathFromLocalConfigurtation(localConfiguration,visualizeUnfoldingStatistics_Plugin.getUnfolding());
		System.out.println(path);
		coloraPath(path,reverseMap, bpmncopia);

		/*for (Transition pn: localConfiguration.get()){
			if (previousNode == null){
				node = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,pn);
				if (node== null){continue;}
				BPMNNode clonato = visualizeUnfoldingStatistics_Plugin.getNodeinClone(bpmncopia, node);
				if (clonato != null){
					clonato.getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getLocalConfigurationColor());
				}

				Color c = mapLocalColor.get(localConfiguration);

				if(c.equals(pal.getDeadColor())){
					Prendo l'arco uscenti dal pn dell'unfolding
					Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = pn.getGraph().getOutEdges(pn);
					for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>   edge :edges ){
						Prendo il PetrinetNode bersaglio dell'arco
						PetrinetNode target = edge.getTarget();
						controllo se e' nella FlowMapBPtoPN per ottenere l'arco BPMN corrispondente 
						for (PetrinetNodeMod u :statistiunf.getFlowMapBPtoPN().keySet()){
							if (u.getLabel()==target.getLabel()){
								BPMNEdge<BPMNNode, BPMNNode> arcoBPMN = statistiunf.getFlowMapBPtoPN().get(u);
								prendo l'arco BPMN della rete clonata
								arcoDead = visualizeUnfoldingStatistics_Plugin.getArcInClone(bpmncopia, arcoBPMN);
								arcoDead.getAttributeMap().put(AttributeMap.EDGECOLOR, pal.getArcDead());							
							}
						}

						}

				}

				previousNode = clonato;
			}
			else{
				node = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,pn);
				BPMNNode clonato = visualizeUnfoldingStatistics_Plugin.getNodeinClone(bpmncopia, node);
				if (clonato != null){
					clonato.getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getLocalConfigurationColor());
				}
				//archi entranti in previousNode, uscenti in clonato
				Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> previousNodeEdges = bpmncopia.getInEdges(previousNode);
				Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> clonatoEdges = bpmncopia.getOutEdges(clonato);
				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> from : previousNodeEdges){
					for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> to : clonatoEdges){
						if (to.getEdgeID() == from.getEdgeID()){							
							to.getAttributeMap().put(AttributeMap.EDGECOLOR, pal.getLocalConfigurationColor());
							to.getAttributeMap().put(AttributeMap.LINEWIDTH, 3.0f);
							to.getAttributeMap().put(AttributeMap.LABELCOLOR, pal.getArcLabelColor());
							archi.add(to);
							break;
						}
					}
				}
				previousNode = clonato;
			}
		}
		Integer max = archi.size();
		Integer arcDead = archi.size() +1;
		for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> a : archi){
			a.getAttributeMap().put(AttributeMap.LABEL, max.toString());
			max--;	
		}

		if(arcoDead != null){
			arcoDead.getAttributeMap().put(AttributeMap.LABEL, arcDead.toString());
			arcoDead.getAttributeMap().put(AttributeMap.LABELCOLOR, pal.getArcDeadLabel());
			arcoDead.getAttributeMap().put(AttributeMap.LINEWIDTH, 3.0f);
			}*/
		return bpmncopia;	
	}


	private void coloraPath(List<PetrinetNode> path, Map<?, BPMNNode> reverseMap, CloneBPMN bpmncopia) {
		Map<PetrinetNodeMod, List<BPMNEdge<BPMNNode, BPMNNode>>> MapPNNodetoArco = statistiunf.getFlowMapBPtoPN();
		HashMap<DirectedGraphElement, DirectedGraphElement> arcToBPMNclonato = bpmncopia.getMapping();
		Integer step = 0;
		for (PetrinetNode element:path){

			BPMNNode node= UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap, element);
			if (node!=null){
				//recupero il nodo clonato
				BPMNNode clonato = visualizeUnfoldingStatistics_Plugin.getNodeinClone(bpmncopia, node);
				//colorare
				if (clonato != null){
					clonato.getAttributeMap().put(AttributeMap.FILLCOLOR, pal.getLocalConfigurationColor());
				}
			}

			PetrinetNodeMod key = new PetrinetNodeMod(element);
			if (MapPNNodetoArco.containsKey(key)){
				//recupero l'arco BPMN
				List<BPMNEdge<BPMNNode, BPMNNode>> arcUnfoldings = MapPNNodetoArco.get(key);
				//Predndo l'arco clonato dalla Mapping
				
				for(BPMNEdge<BPMNNode, BPMNNode> arcUnfolding : arcUnfoldings)
				if (arcToBPMNclonato.containsKey(arcUnfolding)){
					//coloro l'arco BPMN clonato
					arcToBPMNclonato.get(arcUnfolding).getAttributeMap().put(AttributeMap.EDGECOLOR, pal.getLocalConfigurationColor());
					arcToBPMNclonato.get(arcUnfolding).getAttributeMap().put(AttributeMap.LABEL, step.toString());
					arcToBPMNclonato.get(arcUnfolding).getAttributeMap().put(AttributeMap.LABELCOLOR, pal.getArcLabelColor());
					arcToBPMNclonato.get(arcUnfolding).getAttributeMap().put(AttributeMap.LINEWIDTH, 3.0f);
					step++;
				
				}
			}
		}
	}

	private List<PetrinetNode> extractPathFromLocalConfigurtation(LocalConfiguration localConfiguration, Petrinet unfolding) {

		ArrayList<Transition> elementi = localConfiguration.get();
		List<PetrinetNode> path= new ArrayList<PetrinetNode>();
		List<PetrinetNode> res = new ArrayList<PetrinetNode>();

		search(elementi.get(0), path, localConfiguration, res);

		for(int i=0;i<res.size();i++){
			search(res.get(i), path, localConfiguration, res);
		}
		/*PetrinetNode resutl = search(elementi.get(0), path, localConfiguration);
		boolean flag = true;
		while(flag){
			resutl = search(resutl, path, localConfiguration);
			if(resutl==null){
				flag=false;	
			}else
				flag = resutl.getGraph().getInEdges(resutl).size()>0? true: false;
		}*/
		return path;
	}


	private void search(PetrinetNode element, List<PetrinetNode> path, LocalConfiguration localConfiguration, List<PetrinetNode> res ){

		PetrinetNode source = null;
		if(element!=null )
			for( PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arco : element.getGraph().getInEdges(element)){
				source = arco.getSource();
				if(!path.contains(source)){
					path.add(source);				   
					//if 	(arco.getSource().getGraph().getInEdges(source).size() > 1) { 
					//source = arco.getSource();


					//source = arco.getSource();

					if(source.getGraph().getOutEdges(source).size()<2){
						search(arco.getSource(), path, localConfiguration,res);
					}else
						if(!(source instanceof Transition))
							search(arco.getSource(), path, localConfiguration,res);
						else{
							System.out.println(source);
							if(!res.contains(source))
								res.add(source);	
						}
				}				
			}
		if(!res.contains(source))
			res.add(source);

	}

	private void fillmap(ArrayList<Transition> elements, Color color) {
		if (!elements.isEmpty()){					
			for (Transition t : elements) {
				if (!(t.getLabel().equals("reset") || t.getLabel().equals("to") || t.getLabel().equals("ti"))){
					LocalConfiguration l =  local.get(t);
					if(l!=null){
						list.add(l);
						mapLocalColor.put(l, color);
					}
				}												
			}
		}

	}

	private void painttabtrace() {

		this.setBackground(new Color(30, 30, 30));

		JPanel legendPanel = new JPanel();
		legendPanel.setBorder(BorderFactory.createEmptyBorder());
		legendPanel.setBackground(new Color(30, 30, 30));


		tab = new JTable(new AbstractTableModel() {

			private static final long serialVersionUID = -2176731961693608635L;

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				BPMNNode node = null;
				LocalConfiguration runningConf = null; 
				ArrayList<Transition> lista = new ArrayList<Transition>();
				if(list!=null){
					runningConf = list.get(rowIndex);
					lista = runningConf.toArrayList(runningConf);
					for (Transition t: lista){
						node = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap, t);							
						elencoBPMN = listing(reverseMap,lista);
						if (node != null){
							return node.getLabel() +" --> " + elencoBPMN; }
					}
				}		
				return 0;	
			}


			@Override
			public int getRowCount() {
				return list.size();
			}

			@Override
			public int getColumnCount() {

				return 1;
			}

			public String getColumnName(int col) { 

				return "List of history"; 
			}

			public boolean isCellEditable(int row, int col) 
			{ 

				return false; 
			}
		});

		tab.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
			{

				Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);

				LocalConfiguration key = list.get(row);
				Color c = mapLocalColor.get(key);
				setToolTipText(elencoBPMN.toString());
				cell.setBackground(c );
				return cell;

			}
		}
				);

		tab.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
			}



			@Override
			public void mouseClicked(MouseEvent e) {
				JTable target = (JTable)e.getSource();
				int row = target.getSelectedRow();
				LocalConfiguration localConf = list.get(row);
				switch(e.getClickCount()){
				case 2:	if (e.getClickCount() == 2){
					copyStringToClipboard("BPMNlist: " + elencoBPMN + "; Local Configuration: " + localConf.toString()); //copia la localConf
					break;}
				case 1: {
					if (e.getClickCount()==1){
						BPMNDiagram pc = paintConf(localConf,reverseMap);
						visualizeUnfoldingStatistics_Plugin.repaint(false,pc);}
				}
				}	

			}
		});
		legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));

		JScrollPane scrollpane = new JScrollPane(tab); 
		scrollpane.setOpaque(false);
		scrollpane.getViewport().setOpaque(false);
		scrollpane.setBorder(BorderFactory.createEmptyBorder());
		scrollpane.setViewportBorder(BorderFactory.createLineBorder(new Color(10, 10, 10), 2));
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		SlickerDecorator.instance().decorate(scrollpane.getVerticalScrollBar(), new Color(0, 0, 0, 0),
				new Color (140, 140, 140), new Color(80, 80, 80));
		scrollpane.getVerticalScrollBar().setOpaque(false);
		SlickerDecorator.instance().decorate(scrollpane.getHorizontalScrollBar(), new Color(0, 0, 0, 0),
				new Color (140, 140, 140), new Color(80, 80, 80));
		scrollpane.getHorizontalScrollBar().setOpaque(false);
		legendPanel.add(scrollpane,BorderLayout.SOUTH);
		legendPanel.setOpaque(false);
		this.add(legendPanel, BorderLayout.WEST);
		this.setOpaque(false);
	}


	public String listing(Map<PetrinetNodeMod,BPMNNode> reverseMap, ArrayList<Transition> alt){
		elencoBPMN = "";
		BPMNNode node = null;
		ArrayList<BPMNNode> alb = new ArrayList<BPMNNode>();
		for (Transition t: alt){
			//cerco i BPMNNode
			node = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap, t);
			if (node != null){
				alb.add(node);
			}
		}
		//inverto la stringa 
		for (BPMNNode bp: alb){
			elencoBPMN = bp.toString() + ", " + elencoBPMN;
		}
		//rimuovo l'ultima virgola
		return elencoBPMN.substring(0, (elencoBPMN.length())-2); 
	}


	public double getVisWidth() {
		return component.getSize().getWidth();
	}

	public double getVisHeight() {
		return component.getSize().getHeight();
	}

	@Override
	public void paint(Graphics g) {

		super.paint(g);
	}

	public synchronized void mouseDragged(MouseEvent evt) {
	}

	public void mouseClicked(MouseEvent e) {
		JTable target = (JTable)e.getSource();
		int row = target.getSelectedRow();
		int column = target.getSelectedColumn();
		BPMNDiagram bpmncopia = BPMNDiagramFactory.cloneBPMNDiagram(visualizeUnfoldingStatistics_Plugin.getOriginalBpmn());
		visualizeUnfoldingStatistics_Plugin.repaint(false,bpmncopia);

	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public synchronized void mousePressed(MouseEvent e) {

	}

	public synchronized void mouseReleased(MouseEvent e) {

	}

	public void setScalableComponent(ScalableComponent scalable) {
		this.component = scalable.getComponent();
	}

	public void setParent(ScalableViewPanel parent) {

	}

	public JComponent getComponent() {
		return this;
	}

	public int getPosition() {
		return SwingConstants.SOUTH;
	}

	public String getPanelName() {
		return panelName;
	}

	public void setPanelName(String name) {
		this.panelName = name;
	}

	public void updated() {

	}

	public double getHeightInView() {
		return 160;
	}

	public double getWidthInView() {
		return tab.getWidth()+8;
	}

	public void willChangeVisibility(boolean to) {

	}

	public void setSize(int width, int height) {
		super.setSize(width, height);
	}
	public static void copyStringToClipboard (String str)
	{
		Clipboard clipBoard = Toolkit.getDefaultToolkit ().getSystemClipboard ();
		clipBoard.setContents (new StringSelection (str), null);
	}

	public Object fillWidget(int index){
		BPMNNode node = null;
		LocalConfiguration running = null; 
		ArrayList<Transition> lista = new ArrayList<Transition>();
		if(list!=null){
			running = list.get(index);
			lista = running.toArrayList(running);
			for (Transition t: lista){
				node = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap, t);							
				elencoBPMN = listing(reverseMap,lista);
				if (node != null){
					return node.getLabel() +" --> " + elencoBPMN; }
			}
		}
		return 0;				
	}


}


