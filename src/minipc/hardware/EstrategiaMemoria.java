package minipc.hardware;

public enum EstrategiaMemoria {
    FIRST_FIT("Dinámica - First Fit"),
    NEXT_FIT("Dinámica - Next Fit"),
    BEST_FIT("Dinámica - Best Fit"),
    FIJA_IGUAL("Particiones fijas iguales"),
    FIJA_VARIABLE("Particiones fijas diferentes"),
    PAGINACION_FISICA("Paginación física"),
    PAGINACION_VIRTUAL("Paginación virtual - FIFO");

    private final String nombre;
    EstrategiaMemoria(String nombre){ this.nombre = nombre; }
    public boolean esPaginada(){ return this == PAGINACION_FISICA || this == PAGINACION_VIRTUAL; }
    @Override public String toString(){ return nombre; }
}
