package com.talkramer.finalproject.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.talkramer.finalproject.ApplicationStartup;
import com.talkramer.finalproject.model.Domain.Product;
import com.talkramer.finalproject.model.Domain.ProductWrapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class ModelFirebase {

    private Firebase myFirebase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    //authentication docs:
    //https://firebase.google.com/docs/auth/android/password-auth

    ModelFirebase(Context context) {
        Firebase.setAndroidContext(context);
        myFirebase = new Firebase("https://finalproject-23ce7.firebaseio.com/");
        mAuth = FirebaseAuth.getInstance();
        init();
    }

    private void init()
    {
        myFirebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("TAG", "VALUE CHANGED" + dataSnapshot.toString());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d("TAG", "VALUE CANCELLED" + firebaseError);
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("TAG", "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d("TAG", "onAuthStateChanged:signed_out");
                }
            }
        };
        mAuth.addAuthStateListener(mAuthListener);
    }

    public void add(Product pr, Model.AddProductListener listener) {
        try {
            Firebase prRef = myFirebase.child("product").child(pr.getId());

            //use product wrapper for upload product object without Bitmap
            prRef.setValue(new ProductWrapper(pr));

            //TODO: for what using this line?
            listener.done(pr);
        }
        catch (Exception e)
        {
            Log.d("TAG", e.getMessage());
        }
    }

    public void remove(final Product product)
    {
        product.setDeleted(true);
        add(product, new Model.AddProductListener() {
            @Override
            public void done(Product pr) {
                Log.d("TAG", "Product " + product.getId()+ " marked as deleted");
            }
        });
        //Firebase prRef = myFirebase.child("product").child(product.getId());
        //prRef.removeValue();
    }

    public void getProduct(String id, final Model.GetProductsListenerInterface listener) {
        Firebase prRef = myFirebase.child("product").child(id);
        // Attach a listener to read the data at our posts reference
        prRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Product product = dataSnapshot.getValue(Product.class);
                Log.d("TAG", "Successfully load product: " + product.getId());
                List<Product> data = new LinkedList<Product>();
                data.add(product);
                listener.done(data);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed "+ firebaseError.getMessage());
                listener.done(null);
            }
        });

    }

    public void getAllProductsAsync(final Model.GetProductsListenerInterface listener) {
        Firebase prRef = myFirebase.child("product");
        // Attach a listener to read the data at our posts reference
        prRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Product> prList = new LinkedList<Product>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    prList.add(product);
                }
                Log.d("TAG", "read " + dataSnapshot.getChildrenCount() + " new products");
                listener.done(prList);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                listener.done(null);
            }
        });
    }

    public String getUserId(){
        AuthData authData = myFirebase.getAuth();
        if (authData != null) {
            return authData.getUid();
        }


        return null;
    }

    public FirebaseUser getFirebaseUser()
    {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void signUp(String email, String password, final Model.SignupListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(ApplicationStartup.getAppActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("TAG", "createUserWithEmail:onComplete:" + task.isSuccessful());

                        if(task.isSuccessful())
                            listener.success();
                        else
                            listener.fail("Failed to create user. Try again later." + task.getException().getMessage());
                    }
                });
    }

    public void login(String email, String password, final Model.AuthListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(ApplicationStartup.getAppActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Log.d("TAG", "signInWithEmail:onComplete:" + task.isSuccessful());
                            listener.onDone(FirebaseAuth.getInstance().getCurrentUser().getUid(),null);
                        }else{
                            Log.d("TAG", "signInWithEmail:failed", task.getException());
                            listener.onDone(null,task.getException());
                        }
                    }
                });
    }

    public void logout() {
        mAuth.signOut();
    }
}