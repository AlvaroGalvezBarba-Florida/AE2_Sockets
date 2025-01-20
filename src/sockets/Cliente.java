package sockets;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;

/**
 * Esta clase representa un cliente para un chat basado en sockets.
 * El cliente se conecta a un servidor a través de su dirección IP y puerto,
 * y permite al usuario enviar y recibir mensajes en tiempo real.
 */
public class Cliente {
    
    /**
     * Método principal que ejecuta el cliente de chat.
     * El cliente solicita al usuario la dirección IP y el puerto del servidor,
     * establece una conexión con el servidor, y maneja la lectura y escritura de mensajes.
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        try {
            // Crear un BufferedReader para leer la entrada desde la consola
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            
            // Pedir al usuario que ingrese la dirección IP del servidor
            System.out.print("Introduce la IP del servidor: ");
            String serverAddress = consoleReader.readLine();
             
            // Pedir al usuario que ingrese el puerto del servidor
            System.out.print("Introduce el puerto del servidor: ");
            int serverPort = Integer.parseInt(consoleReader.readLine());
             
            // Establecer la conexión con el servidor usando la IP y puerto proporcionados
            Socket socket = new Socket(serverAddress, serverPort);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Leer la respuesta inicial del servidor
            String response;
            boolean first = true;
            while ((response = reader.readLine()) != null) {
                if (first) {
                    System.out.println(response);
                    System.out.print("Selecciona canal: ");
                    first = false;
                } else {
                    System.out.print(response);
                }

                if (response.equals("Presiona ENTER para enviar mensajes")) {
                    break;
                }
                // Leer la entrada del usuario desde la consola y enviarla al servidor
                String input = consoleReader.readLine();
                writer.println(input);
            }

            // Hilo para leer mensajes del servidor en tiempo real
            Thread lectorMensajes = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = reader.readLine()) != null) {
                        // Mostrar el mensaje recibido con un timestamp
                        String timestamp = new SimpleDateFormat("dd/MM-HH:mm:ss").format(new Date());
                        System.out.println();  // Añadir un salto de línea antes del mensaje
                        System.err.println(mensaje); // Mostrar el mensaje en tiempo real
                    }
                } catch (IOException e) {
                    System.err.println("Error leyendo del servidor: " + e.getMessage());
                }
            });

            // Iniciar el hilo que lee mensajes del servidor
            lectorMensajes.start();

            // Hilo principal para enviar mensajes al servidor
            String mensaje;
            while ((mensaje = consoleReader.readLine()) != null) {
                // Usamos JOptionPane para mostrar un cuadro de diálogo para la entrada
                mensaje = JOptionPane.showInputDialog(null, "Introduce 'exit' para cerrar");

                if (mensaje == null) {
                    // Si el usuario ha cancelado o cerrado el cuadro de diálogo, no cerramos la sesión
                    continue;  // Continuamos con la ejecución sin cerrar la sesión
                }
                
                if (mensaje.equalsIgnoreCase("exit")) {
                    writer.println("exit");
                    break;  // Salimos del ciclo si el usuario quiere cerrar
                }

                // Formatear el timestamp y mostrar el mensaje enviado
                String timestamp = new SimpleDateFormat("dd/MM-HH:mm:ss").format(new Date());
                System.out.println(timestamp + ": " + mensaje);

                // Enviar el mensaje al servidor
                writer.println(mensaje);
            }

            // Esperar que el hilo de mensajes del servidor termine
            lectorMensajes.join();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
}
