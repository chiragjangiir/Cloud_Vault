/* Decompiler 4ms, total 256ms, lines 23 */
package src.main.java.com.mountblue.googledrive.repository;

@Repository
public interface FileRepository {
   static {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.repository\" does not match the expected package \"src.main.java.com.mountblue.googledrive.repository\"\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import com cannot be resolved\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.sql cannot be resolved\n\tThe import java.time cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tRepository cannot be resolved to a type\n\tJpaRepository cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tQuery cannot be resolved to a type\n\tSet cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tString cannot be resolved to a type\n\tList cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tString cannot be resolved to a type\n\tQuery cannot be resolved to a type\n\tSet cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tParam cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tParam cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tParam cannot be resolved to a type\n\tString cannot be resolved to a type\n\tParam cannot be resolved to a type\n\tString cannot be resolved to a type\n\tQuery cannot be resolved to a type\n\tSet cannot be resolved to a type\n\tString cannot be resolved to a type\n\tQuery cannot be resolved to a type\n\tList cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tParam cannot be resolved to a type\n\tUsers cannot be resolved to a type\n\tQuery cannot be resolved to a type\n\tList cannot be resolved to a type\n\tFile cannot be resolved to a type\n\tParam cannot be resolved to a type\n\tFolder cannot be resolved to a type\n");
   }

   List<File> findByFolderId(Long var1);

   Set<File> findAllByFileNameContainingIgnoreCase(String var1);

   List<File> findByUserEmail(String var1);

   Set<File> findFilteredFiles(Long var1, Long var2, String var3, String var4);

   Set<String> findAllFileTypes();

   List<File> findFilesByUserInSort(Users var1);

   List<File> findFilesByFolderId(Folder var1);
}
