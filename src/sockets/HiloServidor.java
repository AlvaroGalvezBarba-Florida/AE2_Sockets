package sockets;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HiloServidor implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nombreUsuario;
    private int canalSeleccionado;

    public HiloServidor(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            seleccionarCanalYUsuario();

            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                manejarMensaje(mensaje);
            }
        } catch (IOException e) {
            System.err.println("Error en la conexión con el cliente: " + e.getMessage());
        } finally {
            salirDelCanal();
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando el socket: " + e.getMessage());
            }
        }
    }

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

    private boolean esNombreUnico() {
        for (HiloServidor cliente : Servidor.getCanalClientes().get(canalSeleccionado)) {
            if (cliente.nombreUsuario.equals(nombreUsuario)) {
                return false;
            }
        }
        return true;
    }

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

    private void enviarListaUsuarios() {
        List<String> nombres = new ArrayList<>();
        for (HiloServidor cliente : Servidor.getCanalClientes().get(canalSeleccionado)) {
            nombres.add(cliente.nombreUsuario);
        }
        out.println("Usuarios en el canal: " + nombres);
    }

    private void enviarListaCanales() {
        out.println("Canales disponibles: " + Servidor.getCanales());
    }

    private void enviarMensajeACanal(String mensaje, String timestamp) {
        int canalDestino = Character.getNumericValue(mensaje.charAt(6));
        String contenido = mensaje.substring(8);
        for (HiloServidor cliente : Servidor.getCanalClientes().get(canalDestino)) {
            cliente.out.println(timestamp + ": (canal " + canalSeleccionado + ", " + nombreUsuario + ") >>> " + contenido);
        }
    }

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

    private void salirDelCanal() {
        synchronized (Servidor.getCanalClientes()) {
            if (Servidor.getCanalClientes().get(canalSeleccionado) != null) {
                Servidor.getCanalClientes().get(canalSeleccionado).remove(this);
            }
        }
    }
}
