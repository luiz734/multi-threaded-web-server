import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer {
   public static void main (String argv[]) {
      int port = 7777;

      // Establish the listen socket.
      ServerSocket serverSocket = null;
      try {
         serverSocket = new ServerSocket(port);
      } catch (IOException e1) {
         e1.printStackTrace();
      } 
      // Process HTTP service requests in an infinite loop.
      while (true) {
         // Listen for a TCP connection request.
         try {
            Socket socket = serverSocket.accept(); 
            // Construct an object to process the HTTP request message.
            HttpRequest request = new HttpRequest(socket);
            // Create a new thread to process the request.
            Thread thread = new Thread(request);
            // Start the thread.
            thread.start();
         } catch(Exception e) {
            e.printStackTrace();
         }
      }
   }
}

final class HttpRequest implements Runnable {
   final static String CRLF = "\r\n";
   Socket socket;
   // Constructor
   public HttpRequest(Socket socket) throws Exception {
      this.socket = socket;
   }
   // Implement the run() method of the Runnable interface.
   public void run() {
      try {
         processRequest();
      } catch (Exception e) {
         System.out.println(e);
      }
   }
   private void processRequest() throws Exception {
      InputStream is = socket.getInputStream();
      DataOutputStream os = new DataOutputStream(socket.getOutputStream());
      // Set up input stream filters.
      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      String requestLine = br.readLine();
      System.out.println(requestLine + "\n");
      
      // String headerLine = null;
      // while ((headerLine = br.readLine()).length() != 0) {
      //    System.out.println(headerLine);
      // }

      // Extract the filename from the request line.
      StringTokenizer tokens = new StringTokenizer(requestLine);
      tokens.nextToken(); // skip over the method, which should be "GET"
      String fileName = tokens.nextToken();
      // Prepend a "." so that file request is within the current directory.
      fileName = "." + fileName;

      // Open the requested file.
      FileInputStream fis = null;
      boolean fileExists = true;
      try {
         fis = new FileInputStream(fileName);
      } catch (FileNotFoundException e) {
         fileExists = false;
      }
      
      // Construct the response message.
      String statusLine = null;
      String contentTypeLine = null;
      String entityBody = null;
      if (fileExists) {
         statusLine = "HTTP/1.0 200 OK" + CRLF;
         contentTypeLine = "Content-type: " + contentType( fileName ) + CRLF;
      } else {
         statusLine = "HTTP/1.0 404 NotFound" + CRLF;
         contentTypeLine = "text/html" + CRLF;
         entityBody = 
            "<HTML>" +
            "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
            "<BODY>Not Found</BODY></HTML>";
      }

      // Send the status line.
      os.write(statusLine.getBytes());
      // Send the content type line.
      os.write(contentTypeLine.getBytes());
      // Send a blank line to indicate the end of the header lines.
      os.write(CRLF.getBytes());

      // Send the entity body.
      if (fileExists) {
         sendBytes(fis, os);
         fis.close();
      } else {
         os.write((entityBody).getBytes());
      }

      //close the connection
      os.close();
      br.close();
      socket.close();
   }
   private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
      // Construct a 1K buffer to hold bytes on their way to the socket.
      byte[] buffer = new byte[1024];
      int bytes = 0;
      // Copy requested file into the socket's output stream.
      while((bytes = fis.read(buffer)) != -1 ) {
         os.write(buffer, 0, bytes);   
      }
   }
   private static String contentType(String fileName) {
      if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
         return "text/html";
      }
      if(fileName.endsWith(".gif")) {
         return "image/gif";
      }
      if(fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
         return "image/jpeg";
      }
      return "application/octet-stream";
   } 
}