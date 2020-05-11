public class Main {
    public static void main(String[] args) throws Exception {
        AnnotationManager annotationManager = AnnotationManager.getInstance();

        Human oldHuman = annotationManager.provideManagedObject(Human.class);
        oldHuman.setAge(80);
        oldHuman.pet();

        Human youngHuman = annotationManager.provideManagedObject(Human.class);
        youngHuman.setAge(18);
        youngHuman.pet();
    }
}