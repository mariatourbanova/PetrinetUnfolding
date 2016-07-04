package org.processmining.plugins.unfolding.visualize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.converters.bpmn2pn.InfoConversionBP2PN;
import org.processmining.plugins.unfolding.HistoryUnfolding;
import org.processmining.plugins.unfolding.MyBCSUnfoldingVisualizePlugin;
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
//	private ArrayList<Collection<PetrinetNode>> historyPN;
	private LocalConfigurationMap local;
	private UnfoldingInspectorPanel inspector;
	private StatisticMap statistiunf;
	private MyBCSUnfoldingVisualizePlugin visualizeUnfoldingStatistics_Plugin;
	private Map<LocalConfiguration,Color> maplocalcolor;
	private Map<PetrinetNodeMod,BPMNNode> reverseMap;
	private ArrayList<LocalConfiguration> list;

	public TabTraceUnfodingPanel(PluginContext context, ScalableViewPanel panel, String panelName,
			HistoryUnfolding hu, StatisticMap statistiunf, MyBCSUnfoldingVisualizePlugin visualizeUnfoldingStatistics_Plugin, BPMNDiagram bpmn, InfoConversionBP2PN info, LocalConfigurationMap local ){
		super(new BorderLayout());
		maplocalcolor = new HashMap<LocalConfiguration,Color>();
		list = new ArrayList<LocalConfiguration>();
		this.statistiunf = statistiunf;
		this.visualizeUnfoldingStatistics_Plugin = visualizeUnfoldingStatistics_Plugin;
		this.setBorder(BorderFactory.createEmptyBorder());
		this.setOpaque(true);
		this.setSize(new Dimension(160, 260));
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
	//	historyPN =hu.createHistoryBFS();
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

		fillmap(cutoff,Color.BLUE);
		fillmap(cutoffU,Color.BLUE);
		fillmap(dead,Color.RED);
		fillmap(deadL,Color.RED);		
	}
	
	public BPMNDiagram paintConf(LocalConfiguration localConfiguration,Map<?,BPMNNode> reverseMap){
		BPMNDiagram bpmncopia = BPMNDiagramFactory.cloneBPMNDiagram(MyBCSUnfoldingVisualizePlugin.getOriginalBpmn());
		ArrayList<Transition> elenco = localConfiguration.get();
		for (Transition pn: elenco){
			System.out.println("paint conf: " + pn.getLabel());			
			BPMNNode node = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap,pn);
			BPMNNode clonato = MyBCSUnfoldingVisualizePlugin.getNodeinClone(bpmncopia, node);
			if (clonato != null){
				clonato.getAttributeMap().put(AttributeMap.FILLCOLOR, Color.GREEN);
				}		
		}
		return bpmncopia;	
	}
	
	
	private void fillmap(ArrayList<Transition> elements, Color color) {
		if (!elements.isEmpty()){					
			for (Transition t : elements) {
				if (!(t.getLabel().equals("reset") || t.getLabel().equals("to") || t.getLabel().equals("ti"))){

					LocalConfiguration l =  local.get(t);
					if(l!=null){
						list.add(l);
						maplocalcolor.put(l, color);
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


		//JPanel jp1 = new JPanel();
		tab = new JTable(new AbstractTableModel() {

			private static final long serialVersionUID = -2176731961693608635L;
			
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
//				if(list!=null){
//					return list.get(rowIndex);
//				}
//				return 0;
//			}
//			

				if(list!=null){
					ArrayList<Transition> alt = null;
					BPMNNode node = null;					
					for (LocalConfiguration lc: list){
						
						alt = lc.get();
						for (Transition t:alt){
							node = UtilitiesforMapping.getBPMNNodeFromReverseMap(reverseMap, t);
							if (node != null){
								return node.getLabel();	}
							}
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
				Color c = maplocalcolor.get(key);
				
				cell.setBackground(c );
				return cell;

			}
		}
	);
//			private boolean search(Collection<PetrinetNode> collection,
//					ArrayList<Transition> arraylist) {
//				for (Transition transition : arraylist) {
//					if(collection.contains(transition)){
//						return true;
//					}
//				}
//
//				return false;
//			}
			

		
		

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
				// TODO Auto-generated method stub
				JTable target = (JTable)e.getSource();
				int row = target.getSelectedRow();
				BPMNDiagram pc = paintConf(list.get(row),reverseMap);
				visualizeUnfoldingStatistics_Plugin.repaint(false,pc);
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
		//JTable target = (JTable)e.getSource();
//		int row = target.getSelectedRow();
		//int column = target.getSelectedColumn();
		BPMNDiagram bpmncopia = BPMNDiagramFactory.cloneBPMNDiagram(MyBCSUnfoldingVisualizePlugin.getOriginalBpmn());
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
}