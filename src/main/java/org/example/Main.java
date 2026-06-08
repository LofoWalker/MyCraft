package org.example;

public class Main {
    static void main(String[] args) {
        try (Window window = new Window(1920, 1080, "MyCraft — Step 0")) {
            window.init();

            while (!window.shouldClose()) {
                window.clear();
                window.swapBuffers();
                window.pollEvents();
            }
        }
    }
}
