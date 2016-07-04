package org.processmining.plugins.unfolding;


import java.util.Map;

import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;

public class UtilitiesforMapping {	
	
	/**
	 * Ritorna il BPMNNode corrispondente al PetriNode
	 * @param PetriNode pn
	 * @return BPMNNode
	 */
		public static BPMNNode getBPMNNodeFromReverseMap(Map<?,BPMNNode> reverseMap, PetrinetNode pn){
			BPMNNode nod = null;
			PetrinetNodeMod pnm = new PetrinetNodeMod(pn);
			if(reverseMap.containsKey(pnm)){
					nod = reverseMap.get(pnm);	
			} else 		System.out.println("non trovo pn"); 
			return nod;
		}

}

