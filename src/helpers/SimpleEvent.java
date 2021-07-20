package txrelaysim.src.helpers;

public class SimpleEvent {

	public static final int INV = 1;
	public static final int RECON_REQUEST = 2;
	public static final int SKETCH = 3;
	public static final int RECON_FINALIZATION = 4;
	public static final int SCHEDULED_INV = 5;
	public static final int SCHEDULED_SKETCH = 6;

	private int type;

	public SimpleEvent(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

}
