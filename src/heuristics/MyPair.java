package heuristics;

public class MyPair {
	private int x1;
	private int x2;
	
	public MyPair(int x1, int x2) {
		this.x1 = x1;
		this.x2 = x2;
	}
	
	public int getX1() {
		return this.x1;
	}
	
	public int getX2() {
		return this.x2;
	}
	
	public void setX1(int x1) {
		this.x1 = x1;
	}
	
	public void setX2(int x2) {
		this.x2 = x2;
	}
	
	@Override public String toString() {
		return "(" + this.x1 +  "," + this.x2 + ")";
	}
	
	public String swapToString() {
		int temp = this.getX1();
		this.x1 = this.getX2();
		this.x2 = temp;
	
		return toString();
	}
	
	public MyPair swap() {
		int temp = this.getX1();
		this.x1 = this.getX2();
		this.x2 = temp;
		MyPair p = new MyPair(x1,x2);
		return p;
	}
}
