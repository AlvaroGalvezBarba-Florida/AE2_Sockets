package sockets;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Clase que implementa un hilo en el servidor para gestionar la conexión de un cliente.
 * Este hilo maneja la recepción y envío de mensajes entre el cliente y el canal seleccionado,
 * la asignación de nombre de usuario, la verificación de la unicidad del nombre y la gestión
 * de los comandos del cliente como 'whois', 'channels' y la salida del canal.
 */
public class HiloServidor implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nombreUsuario;
    private int canalSeleccionado;

    /**
     * Constructor de la clase HiloServidor.
     * Se recibe el socket del cliente para establecer la comunicación.
     *
     * @param socket El socket a través del cual se comunica el cliente con el servidor.
     */
    public HiloServidor(Socket socket) {
        this.socket = socket;
    }

    /**
     * Método que ejecuta el hilo para gestionar la comunicación con el cliente.
     * Permite seleccionar un canal, asignar un nombre de usuario, y manejar el envío y
     * recepción de mensajes mientras el cliente esté conectado.
     */
    @Override
    public void run() {
        try {
            // Inicializar los flujos de entrada y salida
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Seleccionar canal y nombre de usuario
            seleccionarCanalYUsuario();

            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                // Manejar los mensajes enviados por el cliente
                manejarMensaje(mensaje);
            }
        } catch (IOException e) {
            System.err.println("Error en la conexión con el cliente: " + e.getMessage());
        } finally {
            // Salir del canal y cerrar el socket al finalizar la comunicación
            salirDelCanal();
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando el socket: " + e.getMessage());
            }
        }
    }

    /**
     * Método que permite seleccionar el canal y asignar el nombre de usuario.
     * Si el canal seleccionado es válido, se pide un nombre de usuario único.
     *
     * @throws IOException Si ocurre un error de entrada/salida durante la selección.
     */
    private void seleccionarCanalYUsuario() throws IOException {
        while (true) {
            out.println("Canales disponibles: " + Servidor.getCanales());
            try {
                canalSeleccionado = Integer.parseInt(in.readLine());
                if (!Servidor.getCanalClientes().containsKey(canalSeleccionado)) {
                    out.println("Canal inválido. Intente nuevamente.");
                    continue;
                }
                out.println("Indica nombre de usuario:");
                setName();
                break;
            } catch (NumberFormatException e) {
                out.println("Entrada no válida. Seleccione un número de canal.");
            }
        }
    }

    /**
     * Método que asigna un nombre de usuario al cliente.
     * Verifica que el nombre sea único y no contenga espacios.
     *
     * @throws IOException Si ocurre un error de entrada/salida durante la asignación del nombre.
     */
    private void setName() throws IOException {
        nombreUsuario = in.readLine();
        if (nombreUsuario.contains(" ")) {
            out.println("El nombre no puede contener espacios. Intente nuevamente.");
        } else if (esNombreUnico()) {
            synchronized (Servidor.getCanalClientes()) {
                Servidor.getCanalClientes().get(canalSeleccionado).add(this);
            }
            out.println("Presiona ENTER para enviar mensajes");
        } else {
            out.println("El nombre ya existe. Intente otro:");
            setName();
        }
    }

    /**
     * Método que verifica si el nombre de usuario es único en el canal seleccionado.
     *
     * @return true si el nombre es único, false en caso contrario.
     */
    private boolean esNombreUnico() {
        for (HiloServidor cliente : Servidor.getCanalClientes().get(canalSeleccionado)) {
            if (cliente.nombreUsuario.equals(nombreUsuario)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Método que maneja los mensajes enviados por el cliente.
     * Dependiendo del tipo de mensaje, se realiza una acción específica.
     *
     * @param mensaje El mensaje enviado por el cliente.
     */
    private void manejarMensaje(String mensaje) {
        String timestamp = new SimpleDateFormat("dd/MM-HH:mm:ss").format(new Date());
        if (mensaje.equalsIgnoreCase("whois")) {
            enviarListaUsuarios();
            enviarMensajeAlCanal(mensaje, timestamp);
        } else if (mensaje.equalsIgnoreCase("channels")) {
            enviarListaCanales();
            enviarMensajeAlCanal(mensaje, timestamp);
        } else if (mensaje.startsWith("@canal")) {
            enviarMensajeACanal(mensaje, timestamp);
            enviarMensajeAlCanal(mensaje, timestamp);
        } else if (mensaje.equalsIgnoreCase("exit")) {
            salirDelCanal();
            enviarMensajeAlCanal(mensaje, timestamp);
        } else {
            enviarMensajeAlCanal(mensaje, timestamp);
        }
    }

    /**
     * Método que envía la lista de usuarios del canal seleccionado al cliente.
     */
    private void enviarListaUsuarios() {
        List<String> nombres = new ArrayList<>();
        for (HiloServidor cliente : Servidor.getCanalClientes().get(canalSeleccionado)) {
            nombres.add(cliente.nombreUsuario);
        }
        out.println("Usuarios en el canal: " + nombres);
    }

    /**
     * Método que envía la lista de canales disponibles al cliente.
     */
    private void enviarListaCanales() {
        out.println("Canales disponibles: " + Servidor.getCanales());
    }

    /**
     * Método que envía un mensaje a un canal específico.
     *
     * @param mensaje El mensaje enviado al canal.
     * @param timestamp La marca de tiempo del mensaje.
     */
    private void enviarMensajeACanal(String mensaje, String timestamp) {
        int canalDestino = Character.getNumericValue(mensaje.charAt(6));
        String contenido = mensaje.substring(8);
        for (HiloServidor cliente : Servidor.getCanalClientes().get(canalDestino)) {
            cliente.out.println(timestamp + ": (canal " + canalSeleccionado + ", " + nombreUsuario + ") >>> " + contenido);
        }
    }

    /**
     * Método que envía un mensaje al canal actual.
     *
     * @param mensaje El mensaje que se enviará al canal.
     * @param timestamp La marca de tiempo del mensaje.
     */
    private synchronized void enviarMensajeAlCanal(String mensaje, String timestamp) {
        List<HiloServidor> clientesCanal = Servidor.getCanalClientes().get(canalSeleccionado);

        if (clientesCanal != null && !clientesCanal.isEmpty()) {
            // Recorremos todos los clientes en el canal
            for (HiloServidor cliente : clientesCanal) {
                // Si no es el cliente que envió el mensaje
                if (!cliente.nombreUsuario.equals(this.nombreUsuario)) {
                    // Enviar el mensaje al cliente
                    cliente.out.println(timestamp + ": " + nombreUsuario + " >>> " + mensaje);
                }
            }
        }

        // Mostrar el mensaje en la consola del servidor para depuración
        System.err.println("SERVIDOR >>> " + nombreUsuario + " (canal " + canalSeleccionado + ") --> " + mensaje);
    }

    /**
     * Método que elimina al cliente del canal y lo desconecta.
     */
    private void salirDelCanal() {
        synchronized (Servidor.getCanalClientes()) {
            if (Servidor.getCanalClientes().get(canalSeleccionado) != null) {
                Servidor.getCanalClientes().get(canalSeleccionado).remove(this);
            }
        }
    }
}
