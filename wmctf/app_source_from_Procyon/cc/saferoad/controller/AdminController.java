// 
// Decompiled by Procyon v0.5.36
// 

package cc.saferoad.controller;

import org.springframework.web.bind.annotation.ResponseBody;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequestMapping({ "/admin" })
public class AdminController
{
    @GetMapping({ "/*" })
    public String Manage() {
        return "manage";
    }
    
    @RequestMapping({ "/{*path}" })
    @ResponseBody
    public void fileDownload(@PathVariable("path") final String path, final HttpServletResponse response) throws IOException {
        final File file = new File("/tmp/" + path);
        final FileInputStream fileInputStream = new FileInputStream(file);
        final InputStream fis = new BufferedInputStream(fileInputStream);
        final byte[] buffer = new byte[fis.available()];
        fis.read(buffer);
        fis.close();
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(path, "UTF-8"));
        response.addHeader("Content-Length", "" + file.length());
        final OutputStream outputStream = new BufferedOutputStream((OutputStream)response.getOutputStream());
        response.setContentType("application/octet-stream");
        outputStream.write(buffer);
        outputStream.flush();
    }
}
