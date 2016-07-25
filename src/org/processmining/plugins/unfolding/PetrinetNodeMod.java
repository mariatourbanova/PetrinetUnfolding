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
			if(pn.getAttributeMap().get("Original id")!=null)
				this.getAttributeMap().put("original_id",pn.getAttributeMap().get("Original id").toString());
		}
	}

	public int hashCode() {
		return getLabel().length();
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
