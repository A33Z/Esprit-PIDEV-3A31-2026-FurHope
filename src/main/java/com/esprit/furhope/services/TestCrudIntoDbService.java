package com.esprit.furhope.services;

import com.esprit.furhope.entities.post;
import com.esprit.furhope.entities.comment;

public class TestCrudIntoDbService {
    public static void main(String[] args) throws Exception {
        postService ps = new postService();
        commentService cs = new commentService();

        post p = new post();
        p.setAuthorId(1);
        p.setCaption("first post");
        p.setMediaType("NONE");
        p.setVisibility("PUBLIC");
        p.setStatus("ACTIVE");
        ps.ajouter(p);

        comment c = new comment();
        c.setPostId(p.getId());
        c.setAuthorId(1);
        c.setParentCommentId(null);
        c.setBody("first comment");
        c.setStatus("ACTIVE");
        cs.ajouter(c);

        System.out.println(p);
        System.out.println(c);
    }
}
