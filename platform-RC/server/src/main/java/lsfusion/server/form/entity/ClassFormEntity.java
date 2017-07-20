package lsfusion.server.form.entity;

import lsfusion.server.logics.BusinessLogics;

public class ClassFormEntity<T extends BusinessLogics<T>> {

    public FormEntity<T> form;
    public ObjectEntity object;

    public ClassFormEntity(FormEntity<T> form, ObjectEntity object) {
        this.form = form;
        this.object = object;
    }
}