package org.openchat;

import static spark.Spark.get;
import static spark.Spark.post;

public class Routes {

    public void create() {
        openchatRoutes();
    }

    private void openchatRoutes() {
        get("status", (req, res) -> "OpenChat: OK!");
        post("users", (req, res) -> "Implementar!");
        post("login", (req, res) -> "Implementar!");
        get("users", (req, res) -> "Implementar!");
        post("users/:userId/timeline", (req, res) -> "Implementar!");
        get("users/:userId/timeline", (req, res) -> "Implementar!");
        post("followings", (req, res) -> "Implementar!");
        get("followings/:followerId/followees", (req, res) -> "Implementar!");
        get("users/:userId/wall", (req, res) -> "Implementar!");
    }
}
