package com.example.concertreservation.user.domain;

import com.example.concertreservation.global.entity.SoftDeletedDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletedDomain {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", unique = true)
  private String email;

  @Embedded
  private Password password;

  @Column(name = "name")
  private String name;

  @Column(name = "nickname", unique = true)
  private String nickName;

  @Column(name = "point")
  private Long point;

  public User(String email, String password, String name, String nickName) {
    this.email = email;
    this.password = Password.hashPassword(password);
    this.name = name;
    this.nickName = nickName;
    this.point = 0L;
  }


}

