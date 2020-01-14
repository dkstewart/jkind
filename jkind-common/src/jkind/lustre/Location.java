package jkind.lustre;

public class Location implements Comparable<Location> {
	
	public final int line;
	public final int charPositionInLine;

	public Location(int line, int charPositionInLine) {
		this.line = line;
		this.charPositionInLine = charPositionInLine;
	}
	
	@Override
	public String toString() {
		return line + ":" + (charPositionInLine+1);
	}
	
	@Override
	public int hashCode() {
		return line * 10000 + charPositionInLine;    // STA : tune the line width
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Location)
			return (((Location) obj).charPositionInLine == charPositionInLine) && (((Location) obj).line == line);
		
		return super.equals(obj);
	}
	
	public static final Location NULL = new Location(0, 0);

	@Override
	public int compareTo(Location other) {
		return this.hashCode() - other.hashCode();
	}
}
