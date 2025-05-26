/* Decompiler 18ms, total 269ms, lines 42 */
package src.main.java.com.mountblue.googledrive.controller;

@Controller
public class FileController {
   private FileService fileService;
   @Autowired
   private ParentFolderService parentFolderService;
   @Autowired
   private UserService userService;
   @Autowired
   private FolderService folderService;

   @Autowired
   public FileController(FileService var1, FolderService var2) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.controller\" does not match the expected package \"src.main.java.com.mountblue.googledrive.controller\"\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.io cannot be resolved\n\tThe import java.io cannot be resolved\n\tThe import java.io cannot be resolved\n\tThe import java.io cannot be resolved\n\tThe import java.nio cannot be resolved\n\tThe import java.nio cannot be resolved\n\tThe import java.security cannot be resolved\n\tThe import java.util cannot be resolved\n\tController cannot be resolved to a type\n\tFileService cannot be resolved to a type\n\tAutowired cannot be resolved to a type\n\tParentFolderService cannot be resolved to a type\n\tAutowired cannot be resolved to a type\n\tUserService cannot be resolved to a type\n\tAutowired cannot be resolved to a type\n\tFolderService cannot be resolved to a type\n\tAutowired cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tFileService cannot be resolved to a type\n\tFolderService cannot be resolved to a type\n\tFileService cannot be resolved to a type\n\tFolderService cannot be resolved to a type\n\tPostMapping cannot be resolved to a type\n\tString cannot be resolved to a type\n\tRequestParam cannot be resolved to a type\n\tMultipartFile cannot be resolved to a type\n\tRequestParam cannot be resolved to a type\n\tString cannot be resolved to a type\n\tModel cannot be resolved to a type\n\tPrincipal cannot be resolved to a type\n\tOAuth2AuthenticationToken cannot be resolved to a type\n\tOAuth2AuthenticationToken cannot be resolved to a type\n\tMap cannot be resolved to a type\n\tString cannot be resolved to a type\n\tObject cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tUsers cannot be resolved to a type\n\tUserService cannot be resolved to a type\n\tString cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tFileService cannot be resolved to a type\n\tParentFolder cannot be resolved to a type\n\tParentFolderService cannot be resolved to a type\n\tSystem cannot be resolved\n\tParentFolderService cannot be resolved to a type\n\tException cannot be resolved to a type\n\tGetMapping cannot be resolved to a type\n\tRequestParam cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tHttpServletResponse cannot be resolved to a type\n\tIOException cannot be resolved to a type\n\tString cannot be resolved to a type\n\tFileService cannot be resolved to a type\n\tString cannot be resolved to a type\n\tUUID cannot be resolved\n\tString cannot be resolved to a type\n\tCredentials cannot be resolved to a type\n\tGoogleCredentials cannot be resolved\n\tFileInputStream cannot be resolved to a type\n\tStorage cannot be resolved to a type\n\tStorageOptions cannot be resolved\n\tBlob cannot be resolved to a type\n\tBlobId cannot be resolved\n\tPaths cannot be resolved\n\tFiles cannot be resolved\n\tPaths cannot be resolved\n\tFiles cannot be resolved\n\tPostMapping cannot be resolved to a type\n\tGetMapping cannot be resolved to a type\n\tPostMapping cannot be resolved to a type\n\tGetMapping cannot be resolved to a type\n");
   }

   public String handleFileUpload(MultipartFile var1, @RequestParam String var2, Model var3, Principal var4) {
      throw new Error("Unresolved compilation problems: \n\tPostMapping cannot be resolved to a type\n\tString cannot be resolved to a type\n\tRequestParam cannot be resolved to a type\n\tMultipartFile cannot be resolved to a type\n\tRequestParam cannot be resolved to a type\n\tString cannot be resolved to a type\n\tModel cannot be resolved to a type\n\tPrincipal cannot be resolved to a type\n\tOAuth2AuthenticationToken cannot be resolved to a type\n\tOAuth2AuthenticationToken cannot be resolved to a type\n\tMap cannot be resolved to a type\n\tString cannot be resolved to a type\n\tObject cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tUsers cannot be resolved to a type\n\tUserService cannot be resolved to a type\n\tString cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tFileService cannot be resolved to a type\n\tParentFolder cannot be resolved to a type\n\tParentFolderService cannot be resolved to a type\n\tSystem cannot be resolved\n\tParentFolderService cannot be resolved to a type\n\tException cannot be resolved to a type\n");
   }

   public void downloadFile(Long var1, HttpServletResponse var2) throws IOException {
      throw new Error("Unresolved compilation problems: \n\tGetMapping cannot be resolved to a type\n\tRequestParam cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tHttpServletResponse cannot be resolved to a type\n\tIOException cannot be resolved to a type\n\tString cannot be resolved to a type\n\tFileService cannot be resolved to a type\n\tString cannot be resolved to a type\n\tUUID cannot be resolved\n\tString cannot be resolved to a type\n\tCredentials cannot be resolved to a type\n\tGoogleCredentials cannot be resolved\n\tFileInputStream cannot be resolved to a type\n\tStorage cannot be resolved to a type\n\tStorageOptions cannot be resolved\n\tBlob cannot be resolved to a type\n\tBlobId cannot be resolved\n\tPaths cannot be resolved\n\tFiles cannot be resolved\n\tPaths cannot be resolved\n\tFiles cannot be resolved\n");
   }

   public String deleteFile(Long var1, @RequestParam String var2, Principal var3) throws IOException {
      throw new Error("Unresolved compilation problem: \n\tPostMapping cannot be resolved to a type\n");
   }

   public String getSearchResult(String var1, Principal var2, Model var3) {
      throw new Error("Unresolved compilation problem: \n\tGetMapping cannot be resolved to a type\n");
   }

   public String starFile(@RequestParam Long var1, @RequestParam String var2, Principal var3, Model var4) {
      throw new Error("Unresolved compilation problem: \n\tPostMapping cannot be resolved to a type\n");
   }

   public void openFile(Long var1, HttpServletResponse var2) throws IOException {
      throw new Error("Unresolved compilation problem: \n\tGetMapping cannot be resolved to a type\n");
   }
}
