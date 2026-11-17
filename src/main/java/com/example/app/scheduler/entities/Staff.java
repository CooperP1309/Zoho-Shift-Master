package com.anticipate.listr.scheduler.entities;

import jakarta.persistence.*;

/*  A person who can be notified about a shift in cliq
 *
 *  NOTE! Is distinct from a user of the system.
 */
@Table(name = "staff")
@Entity
public class Staff
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false)
    private String displayName;

    // Zoho Cliq user ID, used to @mention them in the channel
    @Column(nullable = false, unique = true)
    private String cliqUserId;

    public Integer getId()
    {
        return id;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getCliqUserId()
    {
        return cliqUserId;
    }

    public Staff setId(Integer id)
    {
        this.id = id;
        return this;
    }

    public Staff setDisplayName(String displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public Staff setCliqUserId(String cliqUserId)
    {
        this.cliqUserId = cliqUserId;
        return this;
    }
}