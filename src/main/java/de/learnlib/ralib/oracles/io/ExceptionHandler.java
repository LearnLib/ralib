package de.learnlib.ralib.oracles.io;

//TODO this could be made more general.
public interface ExceptionHandler {
	public static final int NON_DET_ATTEMPTS =3; // how many attempts are made before non determinism is signaled
	public static final int SUL_RESTART_ATTEMPTS =20; // sul restart is normal and should never be a problem
}
