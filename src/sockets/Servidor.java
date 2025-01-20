package sockets;

import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    private static final int PORT = 5000;
    private static final Map<Integer, String> canales = new HashMap<>();
    private static final Map<Integer, List<HiloServidor>> canalClientes = new HashMap<>();

    public static void main(String[] args) {
        cargarCanales();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.err.println("SERVIDOR >>> Arranca el servidor, espera peticiones...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.err.println("SERVIDOR >>> Conexión recibida --> Lanza nuevo hilo");
                new Thread(new HiloServidor(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void cargarCanales() {
        try (BufferedReader br = new BufferedReader(new FileReader("canales.txt"))) {
            String linea;
            int id = 1;
            while ((linea = br.readLine()) != null) {
                canales.put(id, linea);
                canalClientes.put(id, new ArrayList<>());
                id++;
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo canales.txt");
        }
    }

    public static synchronized Map<Integer, String> getCanales() {
        return canales;
    }

    public static synchronized Map<Integer, List<HiloServidor>> getCanalClientes() {
        return canalClientes;
    }
}