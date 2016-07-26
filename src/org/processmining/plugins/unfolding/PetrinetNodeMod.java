package org.processmining.plugins.unfolding;

import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.ExpandableSubNet;

public class PetrinetNodeMod extends PetrinetNode  {

	private String original_id = "";

	public PetrinetNodeMod(
			AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net,
			ExpandableSubNet parent, String label) {
		super(net, parent, label);
	}

	public PetrinetNodeMod(PetrinetNode pn){
		super(pn.getGraph(), pn.getParent(), pn.getLabel());
		if(pn != null){
			if(pn.getAttributeMap().get("Original id")!=null){ 
				original_id = pn.getAttributeMap().get("Original id").toString();
				this.getAttributeMap().put("original_id",original_id);
			}
		}
	}
	
	public PetrinetNodeMod(PetrinetNode pn,String g){
		super(pn.getGraph(), pn.getParent(), pn.getLabel());
		if(pn != null){
			if(pn.getAttributeMap().get("Original id")!=null){ 
				original_id = g;
				this.getAttributeMap().put("original_id",original_id);
			}
		}
	}

	public int hashCode() {
		if(getLabel().length()>0)
			return getLabel().length();
		else
			return original_id.length();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PetrinetNodeMod)) {
			return false;
		}
		PetrinetNodeMod node = (PetrinetNodeMod) o;
		if(original_id.length()>0)
			return original_id.equals(node.getOID());

		return node.getLabel().trim().equals(getLabel().trim());
	}

	public String getOID() {

		return original_id;
	}

}
