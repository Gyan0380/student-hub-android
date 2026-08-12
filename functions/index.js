const {onDocumentCreated}=require('firebase-functions/v2/firestore');
const {initializeApp}=require('firebase-admin/app');
const {getFirestore}=require('firebase-admin/firestore');
const {getMessaging}=require('firebase-admin/messaging');
initializeApp();

async function sendToUsers(userDocs,title,body){
  const db=getFirestore(); const tokens=[];
  for(const u of userDocs){const ts=await u.ref.collection('FcmTokens').get();ts.forEach(t=>{if(t.data().enabled!==false)tokens.push(t.id);});}
  const unique=[...new Set(tokens)];
  for(let i=0;i<unique.length;i+=500){
    const batch=unique.slice(i,i+500); if(!batch.length)continue;
    await getMessaging().sendEachForMulticast({tokens:batch,notification:{title:title||'StudentHub',body:body||''},data:{title:title||'StudentHub',body:body||''}});
  }
}

exports.sendStudentHubNotification=onDocumentCreated('Notifications/{notificationId}',async(event)=>{
  const n=event.data?.data(); if(!n)return;
  const db=getFirestore(); let users=[];
  if(n.toUid==='all') users=(await db.collection('Users').get()).docs;
  else {const d=await db.collection('Users').doc(n.toUid).get();if(d.exists)users=[d];}
  await sendToUsers(users,n.title||'Announcement',n.body||n.text||'');
});

exports.sendStudentHubChatNotification=onDocumentCreated('Chats/{roomId}/Messages/{messageId}',async(event)=>{
  const m=event.data?.data(); if(!m)return;
  const db=getFirestore(); const room=event.params.roomId; let users=[];
  if(room==='global'||room.includes('anonymous')) users=(await db.collection('Users').get()).docs;
  else if(room==='admin-room') users=(await db.collection('Users').where('role','in',['Admin','Owner']).get()).docs;
  else if(room.startsWith('class-')){
    const slug=room.substring(6);
    const snap=await db.collection('Users').get();
    users=snap.docs.filter(u=>{
      const d=u.data(); const access=Array.isArray(d.classAccess)?d.classAccess:[]; const level=String(d.classLevel||'').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');
      return access.some(c=>String(c).toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'')===slug)||level===slug;
    });
  }
  users=users.filter(u=>u.id!==m.senderId);
  await sendToUsers(users,m.senderName?`@${m.senderName}`:'New message',m.text||'📷 New message');
});
