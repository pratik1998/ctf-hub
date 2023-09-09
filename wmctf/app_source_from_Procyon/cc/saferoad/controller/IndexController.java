// 
// Decompiled by Procyon v0.5.36
// 

package cc.saferoad.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class IndexController
{
    @GetMapping({ "/" })
    public String index() {
        return "index";
    }
}
