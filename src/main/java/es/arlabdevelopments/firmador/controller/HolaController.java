package es.arlabdevelopments.firmador.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.okhttp.*;
import es.arlabdevelopments.firmador.Libreria;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Logger;

@Controller
public class HolaController {
    Logger logger = Logger.getLogger("Pruebas SpringBoot");

    @GetMapping("/")
    public String hola() {
        return "hola";
    }

    @PostMapping("/upload")
    public String handleUpload(
            @RequestParam("archivo") MultipartFile file,
            Model model) throws IOException {
        File f = creaFichero(file);
        model.addAttribute("aliases", Libreria.comprobarAlias(f));
        model.addAttribute("archivo", Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));
        return "peticion_datos";
    }

    @PostMapping("/form")
    public String handleForm(
            @RequestParam("seleccion") String alias,
            @RequestParam("contrasena") String contrasena,
            @RequestParam("json") String json,
            @RequestParam("archivo") String file,
            Model model) throws IOException {
        File f = creaFichero(file);
        logger.info("Nombre del fichero: " + f.getName());
        try {
            logger.info("Contenido del fichero: " + Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String privateKey = "-----BEGIN PRIVATE KEY-----" +
                Base64.getEncoder().encodeToString(Libreria.clave(alias, contrasena, f).getEncoded()) + "-----END PRIVATE KEY-----";

        logger.info("Valor de la clave privada: " + privateKey);
        logger.info("Valor del json: " + json);

        String dev = httpPetition(privateKey, json);
        model.addAttribute("data", dev);
        return "muestra_jws";
    }

    private File creaFichero(String file) {
        File f = null;
        try {
            f = File.createTempFile("cert", "p12");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(Base64.getDecoder().decode(file));
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return f;
    }

    private File creaFichero(MultipartFile file) {
        File f = null;
        try {
            f = File.createTempFile("cert", "p12");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(file.getBytes());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return f;
    }
    private String httpPetition(String pem, String json){
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "pem= "+ URLEncoder.encode(pem)+" &json=" + URLEncoder.encode(json));
        Request request = new Request.Builder()
                .url("https://sergio.arlabdevelopments.com:3000/jws")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            logger.severe("Ha habido un error en al ejecución de la llamada al API");
            throw new RuntimeException(e);
        }
        try {
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}