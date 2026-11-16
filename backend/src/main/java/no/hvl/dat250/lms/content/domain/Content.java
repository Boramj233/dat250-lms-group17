package no.hvl.dat250.lms.content.domain;

import jakarta.persistence.*;
import java.time.Instant;
import no.hvl.dat250.lms.module.domain.Module;


@Entity
@Table(name = "contents", uniqueConstraints = @UniqueConstraint(
    name = "uq_content_module_position", columnNames = {"module_id", "content_position"}))
@org.hibernate.annotations.Check(name = "ck_content_position_nonnegative", constraints = "content_position >= 0")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_position", nullable = false)
    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Column(name = "required_for_module_completion", nullable = false)
    private boolean requiredForModuleCompletion;

    public boolean isRequiredForModuleCompletion() { return requiredForModuleCompletion; }

    public void setRequiredForModuleCompletion(boolean required) {
        this.requiredForModuleCompletion = required;
    }

    private Instant visibleFrom;

    private Instant visibleUntil;

    protected Content(){

    }

    protected Content(Module module, String title, int position){
        this.module = module;
        this.title = title;
        changePosition(position);
    }

    public Long getId(){
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void changePosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Content position must be non-negative");
        }
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public Module getModule() {
        return module;
    }

    public Instant getVisibleFrom() {
        return visibleFrom;
    }

    public Instant getVisibleUntil() {
        return visibleUntil;
    }
}
