package com.cloudvault.web.ui;

import com.cloudvault.service.FileService;
import com.cloudvault.service.ObjectStorageService;
import com.cloudvault.service.ShareService;
import com.cloudvault.web.RangeStreamer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

/** Anonymous public share page: token-resolved, server-enforced expiry. */
@Controller
public class PublicShareController {

    private final ShareService shareService;
    private final FileService fileService;
    private final ObjectStorageService objectStorage;

    public PublicShareController(ShareService shareService, FileService fileService,
                                 ObjectStorageService objectStorage) {
        this.shareService = shareService;
        this.fileService = fileService;
        this.objectStorage = objectStorage;
    }

    @GetMapping("/share/{token}")
    public String share(@PathVariable String token, Model model) {
        var share = shareService.resolvePublic(token);
        model.addAttribute("share", share);
        model.addAttribute("token", token);
        model.addAttribute("fmt", com.cloudvault.service.Format.class);
        return "share/view";
    }

    @GetMapping("/share/{token}/download")
    public void download(@PathVariable String token, HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        var share = shareService.resolvePublic(token);
        FileService.Download d = fileService.prepareDownload(null, share.getFile().getId(), token);
        RangeStreamer.write(request, response, d.file().getContentType(), d.file().getName(),
                false, d.version().getSizeBytes(), d.version().getChecksumSha256(),
                () -> objectStorage.openRead(d.object(), 0, -1));
    }
}
