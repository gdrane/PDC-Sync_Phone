package edu.ucla.cens.pdc.phone;

public interface NDNManagerCallback {
	void postProcess(boolean status);
	void postProcess(String text, boolean show_longer);
	void postProcess(String text);
}
