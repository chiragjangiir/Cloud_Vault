/* Decompiler 43ms, total 300ms, lines 71 */
package src.main.java.com.mountblue.googledrive.entity;

@Entity
public class ParentFolder {
   @Id
   @GeneratedValue
   private Long id;
   private String name;
   @OneToMany
   @JoinColumn
   private List<File> files;
   @OneToMany
   @JoinColumn
   private List<Folder> folders;
   @ManyToOne
   @JoinColumn
   private Users user;

   public ParentFolder(Users var1) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tJoinColumn cannot be resolved to a type\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tJoinColumn cannot be resolved to a type\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public Users getUser() {
      throw new Error("Unresolved compilation problem: \n");
   }

   public void setUser(Users var1) {
      throw new Error("Unresolved compilation problem: \n");
   }

   public ParentFolder() {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tJoinColumn cannot be resolved to a type\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tJoinColumn cannot be resolved to a type\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public ParentFolder(Long var1, List<File> var2, List<Folder> var3) {
      throw new Error("Unresolved compilation problems: \n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe type java.lang.Object cannot be resolved. It is indirectly referenced from required .class files\n\tThe declared package \"com.mountblue.googledrive.entity\" does not match the expected package \"src.main.java.com.mountblue.googledrive.entity\"\n\tThe import jakarta cannot be resolved\n\tThe import java.util cannot be resolved\n\tThe import java.util cannot be resolved\n\tEntity cannot be resolved to a type\n\tId cannot be resolved to a type\n\tGeneratedValue cannot be resolved to a type\n\tGenerationType cannot be resolved to a variable\n\tLong cannot be resolved to a type\n\tString cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tJoinColumn cannot be resolved to a type\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tOneToMany cannot be resolved to a type\n\tCascadeType cannot be resolved to a variable\n\tJoinColumn cannot be resolved to a type\n\tList cannot be resolved to a type\n\tArrayList cannot be resolved to a type\n\tManyToOne cannot be resolved to a type\n\tJoinColumn cannot be resolved to a type\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tImplicit super constructor Object() is undefined. Must explicitly invoke another constructor\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public Long getId() {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public void setId(Long var1) {
      throw new Error("Unresolved compilation problems: \n\tLong cannot be resolved to a type\n\tLong cannot be resolved to a type\n");
   }

   public List<File> getFiles() {
      throw new Error("Unresolved compilation problems: \n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public void setFiles(List<File> var1) {
      throw new Error("Unresolved compilation problems: \n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public List<Folder> getFolders() {
      throw new Error("Unresolved compilation problems: \n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public void setFolders(List<Folder> var1) {
      throw new Error("Unresolved compilation problems: \n\tList cannot be resolved to a type\n\tList cannot be resolved to a type\n");
   }

   public String getName() {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }

   public void setName(String var1) {
      throw new Error("Unresolved compilation problems: \n\tString cannot be resolved to a type\n\tString cannot be resolved to a type\n");
   }
}
