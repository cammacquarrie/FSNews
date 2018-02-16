package edu.carleton.comp4601final;

import java.net.URI;
import java.util.ArrayList;

public class HTML {
	private String title;
	private String rightC;
	private String leftC;
	private String footer;

	public HTML() {
		this.title = "";
		this.rightC = "";
		this.leftC = "";
		this.footer = "";
	}

	// setters

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLeftC(String l) {
		this.leftC = l;
	}

	public void setRightC(String r) {
		this.rightC = r;
	}
	public void setFooter(String footer) {
		this.footer = footer;
	}

	// getters

	public String getTitle() {
		return this.title;
	}

	public String getBody() {
		return "<body><div style='float:left;width:50%;min-width:500px;'>" + this.leftC + "</div><div style='float:left;width:50%;min-width:500px'>"
				+ this.rightC + "</div>";
	}

	public String getFooter() {
		return this.footer;
	}

	public String render() {
		String html = "<html><head><title>" + title + "</title></head><body>" + this.getBody() + footer + "</body></html>";
		return html;
	}

	// add features

	public void addToLeftC(String text) {
		this.leftC += text;
	}

	public void addToRightC(String text) {
		this.rightC += text;
	}

	public String formatText(String text, String tag) {
		return "<" + tag + ">" + text + "</" + tag + ">";
	}

	public String formatLink(String text, String url) {
		return "<a href='" + url + "'>" + text + "</a>";
	}
	
	public String formatLink(String text, String url, int f) {
		return "<a style=\"font-size:" + f + "px\" href='" + url + "'>" + text + "</a>";
	}

	public String formatTable(ArrayList<Object> list, boolean theader){
		String html = "<table><tr>";
		if(theader){
			ArrayList<String> row = (ArrayList<String>) list.get(0);
			html += "<tr>";
			for(int i = 0; i < row.size(); i ++){
				html += "<th>" + row.get(i) + "</th>";
			}
			html += "</tr>";
			list.remove(0);
		}
		for(int i = 0; i < list.size(); i ++){
			ArrayList<String> row = (ArrayList<String>) list.get(i);
			html += "<tr>";
			for(int k = 0; k < row.size(); k++){
				html += "<td>" + row.get(k) + "</td>";				
			}
			html += "</tr>";
		}
		html += "</table>";
		return html;
	}

	public String formatTable(ArrayList<String> list, String theader) {
		String html = "<table><tr>";

		html += "<tr>";
		html += "<th>" + theader + "</th>";

		html += "</tr>";

		for (int i = 0; i < list.size(); i++) {
			String row = list.get(i);
			html += "<tr>";
			html += "<td>" + row + "</td>";

			html += "</tr>";
		}
		html += "</table>";
		return html;
	}
}
