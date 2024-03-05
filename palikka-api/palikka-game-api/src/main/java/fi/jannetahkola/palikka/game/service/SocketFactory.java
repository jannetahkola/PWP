package fi.jannetahkola.palikka.game.service;

import java.net.Socket;

public class SocketFactory {
    public Socket newSocket() {
        return new Socket();
    }
}
