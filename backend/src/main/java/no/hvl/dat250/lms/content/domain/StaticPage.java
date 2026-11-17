package no.hvl.dat250.lms.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import no.hvl.dat250.lms.module.domain.Module;

@Entity
@Table(name = "static_pages")
public class StaticPage extends Content {

    @Column(nullable = false, length = 10000)
    private String body;

    protected StaticPage() {}

    public StaticPage(
            Module module,
            String title,
            int position,
            String body) {

        super(module, title, position);
        this.body = body;
    }

    public void updateDetails(String title, String body) {
        if (body == null || body.isBlank() || body.length() > 10000) {
            throw new IllegalArgumentException("Body must be nonblank and at most 10000 characters");
        }
        changeTitle(title);
        this.body = body;
    }

    public String getBody() {
        return body;
    }
}