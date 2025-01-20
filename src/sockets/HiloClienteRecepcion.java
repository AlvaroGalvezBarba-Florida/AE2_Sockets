package sockets;

import java.io.*;

/**
 * Clase que implementa un hilo de recepción de mensajes en el cliente.
 * Este hilo se encarga de leer los mensajes recibidos desde el servidor
 * y mostrarlos en la consola en tiempo real.
 */
public class HiloClienteRecepcion implements Runnable {
    private BufferedReader in;

    /**
     * Constructor de la clase HiloClienteRecepcion.
     * Se pasa un BufferedReader que permitirá leer los mensajes del servidor.
     *
     * @param in El BufferedReader utilizado para leer los mensajes del servidor.
     */
    public HiloClienteRecepcion(BufferedReader in) {
        this.in = in;
    }

    /**
     * Método que se ejecuta en un hilo para recibir los mensajes del servidor.
     * Lee continuamente las respuestas del servidor hasta que se cierre la conexión.
     */
    @Override
    public void run() {
        try {
            String response;
            // Leer los mensajes del servidor y mostrarlos en la consola
            while ((response = in.readLine()) != null) {
                System.err.println(response);  // Imprimir el mensaje recibido en la consola
            }
        } catch (IOException e) {
            // Capturar error si la conexión se cierra o hay problemas con la lectura
            System.err.println("Conexión cerrada.");
        }
    }
}
