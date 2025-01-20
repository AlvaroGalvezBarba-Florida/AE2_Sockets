package sockets;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Clase principal que representa el servidor de chat.
 * Este servidor maneja la creación de canales, la gestión de clientes conectados y la distribución de mensajes.
 */
public class Servidor {
    // Puerto en el que el servidor escuchará las conexiones entrantes
    private static final int PORT = 5000;
    // Mapa que almacena los canales disponibles (por ID) y su nombre
    private static final Map<Integer, String> canales = new HashMap<>();
    // Mapa que almacena los clientes conectados en cada canal (por ID de canal)
    private static final Map<Integer, List<HiloServidor>> canalClientes = new HashMap<>();

    /**
     * Método principal que inicia el servidor y espera conexiones entrantes.
     * Cuando un cliente se conecta, se lanza un nuevo hilo para gestionar la comunicación.
     *
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        // Cargar los canales desde un archivo de texto
        cargarCanales();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.err.println("SERVIDOR >>> Arranca el servidor, espera peticiones...");
            // Bucle infinito para aceptar conexiones entrantes
            while (true) {
                Socket socket = serverSocket.accept();
                System.err.println("SERVIDOR >>> Conexión recibida --> Lanza nuevo hilo");
                // Inicia un nuevo hilo para gestionar la conexión con el cliente
                new Thread(new HiloServidor(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Método que carga los canales desde un archivo de texto llamado "canales.txt".
     * Cada línea del archivo representa un canal, y se asigna un ID secuencial.
     * También se inicializa una lista vacía de clientes para cada canal.
     */
    private static void cargarCanales() {
        try (BufferedReader br = new BufferedReader(new FileReader("canales.txt"))) {
            String linea;
            int id = 1;
            // Lee cada línea del archivo y la agrega como un canal
            while ((linea = br.readLine()) != null) {
                canales.put(id, linea);
                canalClientes.put(id, new ArrayList<>());
                id++;
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo canales.txt");
        }
    }

    /**
     * Método que devuelve el mapa de canales disponibles.
     *
     * @return Un mapa con los canales disponibles, donde la clave es el ID del canal y el valor es el nombre del canal.
     */
    public static synchronized Map<Integer, String> getCanales() {
        return canales;
    }

    /**
     * Método que devuelve el mapa de clientes conectados a los canales.
     *
     * @return Un mapa con los clientes conectados a cada canal, donde la clave es el ID del canal y el valor es una lista de objetos HiloServidor.
     */
    public static synchronized Map<Integer, List<HiloServidor>> getCanalClientes() {
        return canalClientes;
    }
}
