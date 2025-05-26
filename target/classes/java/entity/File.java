/* Decompiler 89ms, total 348ms, lines 110 */
package src.main.java.com.mountblue.googledrive.entity;

@Entity
public class File {
   @Id
   @GeneratedValue
   private Long id;
   private String fileName;
   private String fileType;
   private String link;
   private Long size;
   private boolean isStarred;
   @CreationTimestamp
   private Date uploadDate;
   @ManyToOne
   @JoinColumn
   private Folder folder;
   @ManyToOne
   @JoinColumn
   private Users user;

   public File(Users var1) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tCreationTimestamp cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }

   public Users getUser() {
      throw new Error("Unresolved compilation problem: \n");
   }

   public void setUser(Users var1) {
      throw new Error("Unresolved compilation problem: \n");
   }

   public File(Folder var1) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tCreationTimestamp cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }

   public Folder getFolder() {
      throw new Error("Unresolved compilation problem: \n");
   }

   public void setFolder(Folder var1) {
      throw new Error("Unresolved compilation problem: \n");
   }

   public Long getId() {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public void setId(Long var1) {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public String getFileName() {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public void setFileName(String var1) {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public String getFileType() {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public void setFileType(String var1) {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public String getLink() {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public void setLink(String var1) {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public Long getSize() {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public void setSize(Long var1) {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public Date getUploadDate() {
      throw new Error("Unresolved compilation problems: \n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }

   public void setUploadDate(Date var1) {
      throw new Error("Unresolved compilation problems: \n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }

   public boolean isStarred() {
      throw new Error("Unresolved compilation problem: \n");
   }

   public void setStarred(boolean var1) {
      throw new Error("Unresolved compilation problem: \n");
   }

   public File() {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tCreationTimestamp cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }

   public File(Long var1, String var2, String var3, String var4, Long var5, Date var6) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tCreationTimestamp cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }
}
