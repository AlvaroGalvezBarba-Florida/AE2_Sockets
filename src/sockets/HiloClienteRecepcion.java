package sockets;

import java.io.*;

public class HiloClienteRecepcion implements Runnable {
    private BufferedReader in;

    public HiloClienteRecepcion(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                System.err.println(response);
            }
        } catch (IOException e) {
            System.err.println("Conexión cerrada.");
        }
    }
}