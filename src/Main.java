import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.init();
    }

    public void init() throws IOException{
        ServerSocket server = new ServerSocket(8080);
        var isAlive = true;

        while(isAlive){
            System.out.println("Esperando cliente...");
            var socket = server.accept();
            System.out.println("Conectado!");
            dispatchWorker(socket);
        }
    }

    public void dispatchWorker(Socket socket) throws IOException{
        new Thread(
            ()->{
                try {
                    handleRequest(socket);
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
        ).start();
    }

    public void handleRequest(Socket socket) throws IOException{
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        String line;
        while((line = reader.readLine()) != null && !line.isEmpty()){
            if(line.startsWith("GET")){
                var resource = line.split(" ")[1].replace("/", "");
                System.out.println("El cliente esta pidiendo: " + resource);
                //Enviar la respuesta 
                sendResponse(socket, resource);
            }
        }
    }

    public void sendResponse(Socket socket, String resource) throws IOException {
        var res = new File("resources/" + resource);

        OutputStream out = socket.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        if (res.exists()) {
            String contentType = typeMessage(resource);

            if (contentType.startsWith("image/")) {
                
                writer.write("HTTP/1.0 200 OK\r\n");
                writer.write("Content-Type: " + contentType + "\r\n");
                writer.write("Content-Length: " + res.length() + "\r\n");
                writer.write("Connection: close\r\n");
                writer.write("\r\n");
                writer.flush(); 

                FileInputStream input = new FileInputStream(res);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                
                out.flush();
                input.close();
            } else {
                FileInputStream fis = new FileInputStream(res);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                
                writer.write("HTTP/1.0 200 OK\r\n");
                writer.write("Content-Type: " + contentType + "\r\n");
                writer.write("Content-Length: " + response.length() + "\r\n");
                writer.write("Connection: close\r\n");
                writer.write("\r\n");
                writer.write(response.toString());

                writer.flush();
                br.close();
            }
        } else {
            send404(socket);
        }

        writer.close();
        socket.close();
    }

    public String typeMessage(String resource){

        String line = resource.toString();
        if(line.endsWith(".htm") || line.endsWith(".html")) {
            return "text/html";
        }
        if(line.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if(line.endsWith(".gif")) {
            return "image/gif";
        }
        return "text/html";
    }

    public void send404(Socket socket) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String notFoundMessage = "<html><body><h1>404 Not Found</h1></body></html>";

        writer.write("HTTP/1.0 404 Not Found\r\n");
        writer.write("Content-Type: text/html\r\n");
        writer.write("Content-Length: " + notFoundMessage.length() + "\r\n");
        writer.write("Connection: close\r\n");
        writer.write("\r\n");
        writer.write(notFoundMessage);
        
        writer.flush();
        writer.close();
        socket.close();
    }
}