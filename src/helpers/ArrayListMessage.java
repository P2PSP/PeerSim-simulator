package txrelaysim.src.helpers;

import java.util.ArrayList;

import peersim.core.Node;

public class ArrayListMessage<T> extends SimpleMessage {

	private ArrayList<T> arrayList;

	public ArrayListMessage(int type, Node sender, ArrayList<T> arrayList) {
		super(type, sender);
		this.arrayList = arrayList;
	}

	public ArrayList<T> getArrayList() {
		return this.arrayList;
	}

}
