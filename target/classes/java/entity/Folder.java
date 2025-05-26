/* Decompiler 67ms, total 324ms, lines 78 */
package src.main.java.com.mountblue.googledrive.entity;

@Entity
public class Folder {
   @Id
   @GeneratedValue
   private Long id;
   private String folderName;
   @CreationTimestamp
   private Date createdAt;
   @OneToMany
   private List<File> files;
   @ManyToOne
   @JoinColumn
   private Users user;
   private boolean isStarred;

   public Folder(Users var1) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tCreationTimestamp cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public Users getUser() {
      throw new Error("Unresolved compilation problem: \n");
   }

   public void setUser(Users var1) {
      throw new Error("Unresolved compilation problem: \n");
   }

   public Folder() {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tCreationTimestamp cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public Folder(Long var1, String var2, Date var3) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import org cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tCreationTimestamp cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public Long getId() {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public void setId(Long var1) {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public String getFolderName() {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public void setFolderName(String var1) {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public Date getCreatedAt() {
      throw new Error("Unresolved compilation problems: \n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }

   public void setCreatedAt(Date var1) {
      throw new Error("Unresolved compilation problems: \n\tDate cannot be resolved to a type\n\tDate cannot be resolved to a type\n");
   }

   public List<File> getFiles() {
      throw new Error("Unresolved compilation problems: \n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public void setFiles(List<File> var1) {
      throw new Error("Unresolved compilation problems: \n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public boolean isStarred() {
      throw new Error("Unresolved compilation problem: \n");
   }

   public void setStarred(boolean var1) {
      throw new Error("Unresolved compilation problem: \n");
   }
}
