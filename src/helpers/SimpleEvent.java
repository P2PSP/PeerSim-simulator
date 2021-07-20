package txrelaysim.src.helpers;

public class SimpleEvent {

	public static final int INV = 1;
	public static final int GETDATA = 2;
	public static final int RECON_REQUEST = 3;
	public static final int SKETCH = 4;
	public static final int RECON_FINALIZATION = 5;
	public static final int SCHEDULED_INV = 6;
	public static final int SCHEDULED_SKETCH = 7;

	private int type;

	public SimpleEvent(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

}
