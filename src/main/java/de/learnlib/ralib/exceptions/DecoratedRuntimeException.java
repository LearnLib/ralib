package de.learnlib.ralib.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A runtime exception that can be easily variable valuations at the
 * moment of the exception that help debug it.
 */
public class DecoratedRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private List<Decoration> decorations;
	
	public DecoratedRuntimeException() {
		decorations = new ArrayList<Decoration>();
	}
	
	public DecoratedRuntimeException(String msg) {
		super(msg);
		decorations = new ArrayList<Decoration>();
	} 
	
	public DecoratedRuntimeException addDecoration(String description, Object decorativeObject) {
		decorations.add(new Decoration(description, decorativeObject));
		return this;
	}
	
	// adds the surpressed exception.
	public DecoratedRuntimeException addSuppressedExc(Exception exception) {
		this.addSuppressed(exception);
		return this;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Decoration decoration : decorations) {
			builder.append(decoration.toString()).append("\n");
		}
		builder.append(super.toString());
		return builder.toString();
	}
	
	private static class Decoration {
		private Object decoration;
		private String description;
		public Decoration(String description, Object decoration) {
			super();
			this.decoration = decoration;
			this.description = description;
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(description).append(":");
			if (decoration instanceof Collection) {
				builder.append("\n");
				for(Object element : ((Collection<?>)decoration)) {
					builder.append(element).append("\n");
				}
			} else {
				builder.append(decoration);
			}
			return builder.toString();
		}
	}
}
