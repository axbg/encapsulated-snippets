public class Human {
    private int age;
    private Cat cat;

    public void setAge(int age) {
        this.age = age;
    }

    public void pet() {
        String result;
        result = cat.calculateDisposition(age) > 10 ? "Auch! This surely hurt!" : "The cat seems to like you!";
        System.out.println(age + ": " + result);
    }
}