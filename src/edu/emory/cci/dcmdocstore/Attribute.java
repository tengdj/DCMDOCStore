package edu.emory.cci.dcmdocstore;

import java.util.ArrayList;

public class Attribute {
     private String tag;
     private String vr;
     private String description;
     private ArrayList<String> attr = new ArrayList<String>();
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getVr() {
		return vr;
	}
	public void setVr(String vr) {
		this.vr = vr;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public ArrayList<String> getAttr() {
		return attr;
	}
	public void setAttr(ArrayList<String> attr) {
		this.attr = attr;
	}
     
	
}
